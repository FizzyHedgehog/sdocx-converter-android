use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jdouble, jint};
use std::path::Path;

#[no_mangle]
pub extern "system" fn Java_com_fizzyhedgehog_sdocxconverter_NativeConverter_convertNative(
    mut env: JNIEnv,
    _class: JClass,
    input_path: JString,
    output_path: JString,
    _pen_scale: jdouble,
    _marker_scale: jdouble,
    _split_option: JString,
    _auto_split: jboolean,
) -> jint {
    let input: String = match env.get_string(&input_path) {
        Ok(s) => s.into(),
        Err(_) => return -1,
    };
    let output: String = match env.get_string(&output_path) {
        Ok(s) => s.into(),
        Err(_) => return -2,
    };

    let in_path = Path::new(&input);
    let _out_path = Path::new(&output);

    if !in_path.exists() {
        return -3;
    }

    // sdocx2pdf core vector conversion logic will execute here
    // return 0 for success
    0
}
