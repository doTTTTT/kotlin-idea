# kotlin-idea

### Extension


```kotlin
fun Activity.start(clazz: KClass<out Activity>, callback: Intent.() -> Unit = {}) {
  startActivity(Intent(this, clazz.java).apply(callback))
}
```

Use 

```kotlin
start(SomeActivity::class) {
  putExtra("SOME_EXTRA", "extra")
}
```
