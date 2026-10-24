use std::path::Path;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jdouble, jint};
use sdocx2pdf::{BasicSplitMode, convert_sdocx_to_pdf};

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

    let result = std::panic::catch_unwind(|| {
        convert_sdocx_to_pdf(
            in_path,
            out_path,
            pen_scale as f32,
            marker_scale as f32,
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
