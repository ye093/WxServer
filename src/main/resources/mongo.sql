#插入
post =
{
  "title" : "My Blog Post",
  "content" : "Here's my blog post.",
  "date" : new Date()
}

db.blog.insert(post)

#返回所有文档
db.blog.find()
#返回一个文档
db.blog.findOne()

#更新
#为post增加一个评论字段
post.comments = []
#执行更新操作
db.blog.update({title: "My Blog Post"}, post)

#删除
db.blog.remove({title: "My Blog Post"})

#修改里边某个数字增加1
#例如:users {"username" : "joe", "relationships": {"age": 33}}
db.users.update({"username": "joe"}, {"$inc" : {"relationships.age" : 1}})#执行后age=34

#总结
1)属性增加用$inc
  db.users.update({"name" : "yejy"}, {"$inc" : {"age" : 1}}) #users集合里name = yejy的obj中的age增加1
  扩展用法: db.users.update({"name" : "yejy"}, {"$inc" : {"message.len" : 1}}) "message"中的len属性增加1
2)为文档设置属性用$set,存在则修改,不存在则新增
  db.users.update({"name" : "yejy"}, {"$set" : {"age" : 22}}) #新增或修改属性age
3)删除某个属性用$unset
  db.users.update({"name" : "yejy"}, {"$unset" : {"age": 1}}) #age属性被删除了
4)为文档中的数组项新增用$push,没有则创建一个数组
  db.users.update({"name" : "yejy"}, {"$push" : {"comments" : "good day!!"}})
5)为文档中的数组新增,不存在时增,存在则不做任何操作$addToSet
  db.users.update({"name" : "yejy"}, {"$addToSet" : {"comments" : "nice day!!!"}})
6)$addToSet与$each结合起来 为数组添加多个不重复的值
  db.users.update({"name" : "yejy"}, {"$addToSet" : {"comments" : {"$each" : ["good day!!", "nice day!!!", "Hello"]}}})
7)删除数组头或尾一项用$pop, 1表示尾, -1表示头
  db.users.update({"name" : "yejy"}, {"$pop" : {"comments" : 1}}) #从尾部删除一条评论
  db.users.update({"name" : "yejy"}, {"$pop" : {"comments" : -1}}) #从数组头部删除一项
8)删除配置的项用$pull
  db.user.update({"name" : "yejy"}, {"$pull": {"comments" : "good day!!!"}})
  //20170915
9)数组下标操作
  db.blog.posts.update({ "_id" : ObjectId("59bb2f7c0861a270d1a12e97")}, {"$inc" : {"comments.0.votes" : 1}})#comments数组的第一项的评论数增加1
   db.blog.posts.update({"comments.author" : "John"}, {"$set" : {"comments.$.author" : "Jim"}})
10)update操作中的第3个参数表示upsert, true（有则更新，无则添加）,false默认匹配则更改，否则不做任何操作
  db.blog.update({"username": "yejy"}, {"$push" : {"emails" : "yeyeye093@gmail.com"}}, true)
11)save函数
   var pp = db.blog.findOne({"username" : "yejy"})
   pp.username = "yejinyun";
   db.blog.save(pp);
12)更新多个文档,update操作中第4个参数设置为true
  db.games.update({"game" : "pinball"}, {"$unset" : {"other" : 1}}, false, true)
13)返回上次操作的结果
  db.runCommand({getLastError: 1})
14)查找并更新
  db.runCommand({"findAndModify" : "games", "query" : {"name" : "Tomcat"}, "sort": {"score" : 1}, "update" : {"$set" : {"name" : "Tome"}}})
15)查找并删除
  db.runCommand({"findAndModify" : "games", "query" : {"name" : "Tomcat"}, "sort": {"score" : 1}, "remove" : true})
16)查找 列出指定字段
   db.blog.find({}, {"username": 1, "_id" : 0})#列出username并过滤掉_id
17)聚合函数aggregate
  分组$group
  基本用法:db.testColl.aggregate([{$group: {_id: {name: "$name"}, age_avg: {$avg: "$age"}}}])#以name分组并统计age平均值

