use std::path::Path;
use jni::JNIEnv;
use jni::objects::{JClass, JObjectArray, JString};
use jni::sys::{jboolean, jdouble, jint};
use sdocx2pdf::{BasicSplitMode, convert_sdocx_to_pdf_with_options, merge_pdfs};

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_fizzyhedgehog_sdocxconverter_NativeConverter_convertNative(
    mut env: JNIEnv,
    _class: JClass,
    input_path: JString,
    output_path: JString,
    pen_scale: jdouble,
    marker_scale: jdouble,
    marker_opacity: jdouble,
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

    let result = std::panic::catch_unwind(|| {
        convert_sdocx_to_pdf_with_options(
            in_path,
            out_path,
            pen_scale as f32,
            marker_scale as f32,
            marker_opacity as f32,
            auto_split != 0,
            basic_split,
            false,
        )
    });

    match result {
        Ok(Ok(_)) => 0,
        Ok(Err(_)) => 1,
        Err(_) => -3, // Engine panicked: caught safely without crashing the JVM
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_fizzyhedgehog_sdocxconverter_NativeConverter_mergePdfsNative(
    mut env: JNIEnv,
    _class: JClass,
    input_paths: JObjectArray,
    output_path: JString,
) -> jint {
    let out_str: String = match env.get_string(&output_path) {
        Ok(s) => s.into(),
        Err(_) => return -1,
    };

    let len = match env.get_array_length(&input_paths) {
        Ok(l) => l,
        Err(_) => return -2,
    };

    let mut string_paths = Vec::with_capacity(len as usize);
    for i in 0..len {
        let obj = match env.get_object_array_element(&input_paths, i) {
            Ok(o) => o,
            Err(_) => return -3,
        };
        let j_str: JString = obj.into();
        let s: String = match env.get_string(&j_str) {
            Ok(s) => s.into(),
            Err(_) => return -4,
        };
        string_paths.push(s);
    }

    let path_refs: Vec<&Path> = string_paths.iter().map(|s| Path::new(s.as_str())).collect();
    let out_p = Path::new(&out_str);

    let result = std::panic::catch_unwind(|| {
        merge_pdfs(&path_refs, out_p)
    });

    match result {
        Ok(Ok(_)) => 0,
        Ok(Err(e)) => {
            eprintln!("merge_pdfs error: {e:?}");
            1
        }
        Err(p) => {
            eprintln!("merge_pdfs panicked: {p:?}");
            -5
        }
    }
}
