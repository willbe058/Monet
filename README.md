#[WARNING] I am now refactoring this whole project, so ignore all the information below cause the api may change.


# Monet
a simple and easy-use image loader, just for my personal learning

##Usage

- just add some lines in your project build.gradle

```gradle
allprojects {

		repositories {
			...
			maven { url "https://www.jitpack.io" }
		}
	}
	
```

- and your app build.gradle

```gradle
dependencies {
	        compile 'com.github.willbe058:monet:0.1.1'
	}
	
```

and use it in your project
```java
	
	Monet.build(context).draw(url,imageview,100,100);

```
that's it! 
