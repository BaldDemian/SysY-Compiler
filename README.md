# SysY-Compiler

## 环境配置

- 必要配置：LLVM，Java >= 11, IDEA

- LLVM 安装

  - Linux

    ```shell
    sudo apt-get install llvm
    sudo apt-get install clang
    ```
    
    输入以下命令有提示即成功
    
      ```shell
      clang -v
      lli --version
      ```
    
  - Windows
  
    [Release LLVM 14.0.0 · llvm/llvm-project (github.com)](https://github.com/llvm/llvm-project/releases/tag/llvmorg-14.0.0) 中选择对应的 exe 文件下载并执行
  

## 运行

```shell
git clone https://github.com/BaldDemian/SysY-Compiler.git
```

在 IDEA 中以项目形式打开，运行 Main 类中的 main 方法

项目根目录下的 test.txt 存储 sysy 源代码，res.txt 输出相应的 LLVM IR。检测出的错误信息在标准错误流中，高亮及格式化后的代码输出在标准输出流中

## 样例说明

- 对于一个含有词法错误的源代码

  ```c
  int main(){
    int i = 1;
    int j = ~i;
  }
  
  ```

  输出如下：

  ```
  Error type A at Line 3: token recognition error at: '~'
  ```

- 对于一个含有语法错误的源代码

  ```
  int main() 
      a = 3;
      int b = add(3, 4);
  
  ```

  输出如下：

  ```
  Error type B at Line 2: missing '{' at 'a'
  Error type B at Line 4: extraneous input '<EOF>' expecting {'const', 'int', 'if', 'while', 'break', 'continue', 'return', '+', '-', '!', '(', '{', '}', ';', IDENT, INTEGR_CONST}
  ```

- 对于如下一个简单的源代码

  ```c
  int main(){int a = 3;
  
      }
  ```

  标准输出将输出高亮和格式化后的代码：

  ![](https://blog-1314070381.cos.ap-nanjing.myqcloud.com/img/demo1.png)

  LLVM IR 如下

  ```llvm
  ; ModuleID = 'moudle'
  source_filename = "moudle"
  
  define i32 @main() {
  mainEntry:
    %"1" = alloca i32, align 4
    store i32 3, i32* %"1", align 4
    ret i32 0
  }
  ```

- 对于如下一个存在语义错误的源代码

  ```c
  int main() {
      a = 3;
      int b = add(3, 4);
  }
  ```

  输出如下

  ![](https://blog-1314070381.cos.ap-nanjing.myqcloud.com/img/demo2.png)

## 简介

- 本项目是为 SysY 语言（一个 C 语言的简单子集）编写的编译器前端。功能包括检查词法、语法、语义错误，为无词法语法错误的源代码文件进行高亮和格式化，最终生成 LLVM IR。目前已完成对于表达式、变量（含常量）声明与定义、函数定义与调用以及简单语句的翻译，后续将增加对分支、循环语句的翻译。对于上述支持的翻译，本项目输出的 IR 与使用 clang 对相同源代码的 C 语言编译的结果是相似的
- 本项目使用了 ANTLR v4 自动生成词法分析器和语法分析器，并使用 javacpp 项目中的 LLVM API 生成 LLVM IR
- 本项目是对南京大学软件学院 2022 年秋季学期和 2024 年春季学期**编译原理**课程多次课程实验的汇总。如果您是正在学习该课程的学生，无论出于什么目的，基于学术诚信，都请**不要**查看本项目的源代码

