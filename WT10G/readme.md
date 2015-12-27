# 输入数据说明
- topics.txt: 451-550.topics可能是后缀原因，打开出错，因为更改了文件名。另外，510行时原文件<title>　行为空，用行解析出错，因此要进行解析修正。

- WT10G中有文件<title>域不能按行处理，同时还伴有语法错误。如：
```
<title>
xxx
</title>

<title> xxx
</title>

<title> xxx</title
```