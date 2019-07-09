# kotlin-idea

### Extension

```
fun Activity.start(clazz: KClass<out Activity>, callback: Intent.() -> Unit = {}) {
  startActivity(Intent(this, clazz.java).apply(callback))
}
```
