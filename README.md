
## 编写目的
最近在使用ElasticSearch（以下简称ES）搭建实时数仓的时候，对于索引中的数据都是编写代码或者查询语句进行搜索，效率低下。于是想着把搜索过程进行封装，并将搜索结果直接展示在前端。

**图1：数据列表页面**
![数据列表页面](https://img-blog.csdnimg.cn/20201221225245906.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L21ybGlxaWZlbmc=,size_16,color_FFFFFF,t_70)
**图2：数据详情页面**
![数据详情页面](https://img-blog.csdnimg.cn/20201221225333143.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L21ybGlxaWZlbmc=,size_16,color_FFFFFF,t_70)


## 功能介绍与亮点
1、用户可以直接在搜索框内输入数据，即可对ES库进行搜索，搜索结果将通过列表方式展示。其中列表字段可以自行配置（后续会讲具体配置方法）。
2、添加时间过滤与排序功能，用户可以直接在页面选定时间段对数据进行过滤与排序。前提是数据中需要存在时间字段并且进行配置。
3、数据分类展示，方便检索与查看相对应的数据。
4、高级搜索功能，可以在搜索词内输入“&&”或“||”进行“且与或”的逻辑搜索，相当于MySQL中的“or和and”。同时用户可以使用“people.name=李奇峰”即可指定搜索的具体字段。

## 使用方法
1、将代码同步至本地后，找到application.yml并修改其中的配置，将数据替换成你本地的样式。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201221232454756.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L21ybGlxaWZlbmc=,size_16,color_FFFFFF,t_70)
2、代码同步后，找到search.sql文件并导入至mysql库中（需要提前建好库，库名随意），导入成功后出现两张表：table_info、field_info。
其中table_info表结构如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201221234407255.png)
field_info表结构如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020122123464176.png)
## 项目地址
[https://github.com/mrliqifeng/searchWebForEs](https://github.com/mrliqifeng/searchWebForEs)
