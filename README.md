# swordfish 编译方法

## 1.下载 swordfish 源码
```
$ git clone https://github.com/baifendian/swordfish.git
```

## 2.通过maven打包
```
  $ cd  {swordfish_source_code_home}
  $ mvn -U clean package assembly:assembly -Dmaven.test.skip=true
```

打包成功，日志显示如下：
```
  [INFO] BUILD SUCCESS
  [INFO] -----------------------------------------------------------------
  [INFO] Total time: 4.385 s
  [INFO] Finished at: 2017-02-27T16:13:50+08:00
  [INFO] Final Memory: 52M/314M
  [INFO] -----------------------------------------------------------------
```

在 target 目录下看到 swordfish-all-{version}, 这个目录下面为打包好的项目信息

# 如何部署
[服务部署手册](https://github.com/baifendian/swordfish/wiki/deploy)

# 插件开发
[插件开发手册](https://github.com/baifendian/swordfish/wiki/plugin-dev)

# Restful API
[API 说明](https://github.com/baifendian/swordfish/wiki)
