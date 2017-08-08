### releasing steps

```
jenv shell 1.7
sbt
> clean
> publish

jenv shell 1.8
sbt
> clean
> ^^1.0.0-RC3
> publish
```
