#!/bin/bash

echo "=== 传送带连接器模组构建脚本 ==="
echo ""

# 检查Java版本
echo "检查Java版本..."
java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$java_version" -lt 17 ]; then
    echo "错误: 需要Java 17或更高版本，当前版本: $java_version"
    exit 1
fi
echo "Java版本检查通过: $java_version"

# 检查Maven
echo "检查Maven..."
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven，请先安装Maven"
    exit 1
fi
echo "Maven检查通过"

# 清理项目
echo "清理项目..."
mvn clean

# 编译项目
echo "编译项目..."
mvn compile

if [ $? -eq 0 ]; then
    echo "编译成功!"
else
    echo "编译失败!"
    exit 1
fi

# 运行测试
echo "运行测试..."
mvn test

if [ $? -eq 0 ]; then
    echo "测试通过!"
else
    echo "测试失败!"
    exit 1
fi

# 打包项目
echo "打包项目..."
mvn package

if [ $? -eq 0 ]; then
    echo "打包成功!"
    echo "生成的jar文件位置: target/conveyor-connector-1.0.0.jar"
else
    echo "打包失败!"
    exit 1
fi

echo ""
echo "=== 构建完成 ==="
echo "请将生成的jar文件放入Minecraft的mods文件夹中"
echo "确保已安装Forge 47.1.0+和机械动力模组0.5.1.f"