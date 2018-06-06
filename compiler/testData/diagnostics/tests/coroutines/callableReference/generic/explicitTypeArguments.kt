// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER

fun <T> takeFun(f: suspend (T) -> Unit) {}
fun <T, R> callFun(f: suspend (T) -> R): R = TODO()

suspend fun <T> foo(s: T) {}
suspend fun <T : <!FINAL_UPPER_BOUND!>Int<!>> fooInt(s: T) {}

open class Wrapper<T>(val value: T)
suspend fun <T, R : Wrapper<in T>> createWrapper(s: T): R = TODO()

fun <T> Wrapper<T>.baz(transform: suspend (T) -> Unit): T = TODO()

fun test() {
    takeFun<String>(::foo)
    takeFun<String>(<!NI;TYPE_MISMATCH!>::<!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>fooInt<!><!>)

    callFun<String, Wrapper<String>>(::createWrapper)
    callFun<Int, Wrapper<Number>>(::createWrapper)
    callFun<String, Wrapper<*>>(::createWrapper)
    callFun<String, Wrapper<Int>>(::<!OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>createWrapper<!>)

    callFun<Int, Wrapper<Int>>(::createWrapper).baz(::foo)
}
