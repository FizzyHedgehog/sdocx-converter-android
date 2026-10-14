use std::path::Path;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jdouble, jint};
use itertools::Itertools;
use krilla::{
    geom::{Size, Transform},
    metadata::Metadata,
    page::PageSettings,
};
use indicatif::MultiProgress;
use sdocx::Document;

mod page;
mod pdf;
mod shape;
mod stroke;
mod tool;

use page::{EmbedMap, PageConversionCtx};

#[derive(Clone, Copy, Debug)]
pub enum BasicSplitMode {
    IsoPortrait,
    IsoLandscape,
}

pub fn convert_sdocx_to_pdf(
    in_path: &Path,
    out_path: &Path,
    pen_multiplier: f32,
    marker_multiplier: f32,
    auto_split: bool,
    basic_split: Option<BasicSplitMode>,
) -> anyhow::Result<()> {
    let (document, mut media_storage) = match Document::from_zip(in_path) {
        Ok(v) => v,
        Err(_) => Document::from_dir(in_path)?,
    };

    let document_name = document
        .title_text()
        .raw_string()
        .map(|s| s.trim())
        .filter(|s| !s.is_empty())
        .unwrap_or("Samsung Notes");

    let mut pdf = krilla::Document::new();
    let embed_map = EmbedMap::new(&document, &mut media_storage)?;
    let pages = page::split_document_into_pages(&document, &embed_map, auto_split, basic_split).collect_vec();
    let embedded_documents = embed_map.into_documents();

    let multi_progress = MultiProgress::new();

    for page in pages {
        let output_size = page.output_size();
        let mut pdf_page = pdf.start_page_with(PageSettings::new(
            Size::from_wh(output_size.width as f32, output_size.height as f32)
                .expect("page size calculation failed"),
        ));

        let mut page_ctx = PageConversionCtx::new(&page, pdf_page.surface());

        if let Some([b, g, r, a]) = page.background_colour() {
            page_ctx.fill_background(r, g, b, a);
        }

        for embed in page.embeds() {
            let src_page_index = embed.page_index() as usize;
            let dest_rect = embed.rect();
            let dest_width = dest_rect.width();
            let dest_height = dest_rect.height();

            if let Some(dest_size) = Size::from_wh(dest_width as f32, dest_height as f32) {
                page_ctx.surface.push_transform(&Transform::from_row(
                    1.0, 0.0, 0.0, 1.0,
                    dest_rect.min.x as f32,
                    dest_rect.min.y as f32,
                ));

                if let Some(doc) = embedded_documents.get(embed.file().name()) {
                    page_ctx.surface.draw_pdf_page(doc, dest_size, src_page_index);
                }

                page_ctx.surface.pop();
            }
        }

        for inline_obj in page.inline_objects() {
            let _ = page_ctx.draw_single_object(
                &inline_obj.object,
                pen_multiplier,
                marker_multiplier,
                &mut media_storage,
            );
        }

        page_ctx.draw_objects(
            page.objects(),
            pen_multiplier,
            marker_multiplier,
            &multi_progress,
            &mut media_storage,
        );
    }

    pdf.set_metadata(
        Metadata::new()
            .title(document_name.to_owned())
            .producer("SDOCX Converter Lite".to_owned())
            .creator("Samsung Notes".to_owned()),
    );

    let pdf_bytes = pdf.finish()?;
    std::fs::write(out_path, &pdf_bytes)?;

    Ok(())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_fizzyhedgehog_sdocxconverter_NativeConverter_convertNative(
    mut env: JNIEnv,
    _class: JClass,
    input_path: JString,
    output_path: JString,
    pen_scale: jdouble,
    marker_scale: jdouble,
    split_option: JString,
    auto_split: jboolean,
) -> jint {
    let input: String = match env.get_string(&input_path) {
        Ok(s) => s.into(),
        Err(_) => return -1,
    };
    let output: String = match env.get_string(&output_path) {
        Ok(s) => s.into(),
        Err(_) => return -2,
    };
    let split_str: String = match env.get_string(&split_option) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    let basic_split = match split_str.as_str() {
        "iso-portrait" => Some(BasicSplitMode::IsoPortrait),
        "iso-landscape" => Some(BasicSplitMode::IsoLandscape),
        _ => None,
    };

    let in_path = Path::new(&input);
    let out_path = Path::new(&output);

    match convert_sdocx_to_pdf(
        in_path,
        out_path,
        pen_scale as f32,
        marker_scale as f32,
        auto_split != 0,
        basic_split,
    ) {
        Ok(_) => 0,
        Err(_) => 1,
    }
}
