配置规范

jing项目中，与配置有关的部分包含以下几类

## 固定的配置

固定的配置通常情况下是静态常量，他们直接被写死在代码中，并被赋予了一个在大多数情况下都较为合适的值

关于这类情况，典型的例子是ArrayList中的初始容量为10,这个值是被固定在jdk中且不能修改的，并且它大小适中，使得在大多数情况下，我们都不需要手动指定数组初始容量

通常，开发者不应该去修改固定配置的内容，但默认配置终究是不能涵盖所有的情况的，对于此类配置有一个特定的注解对其进行标记，
修改之后会产生的相关后果会被标明在其javadoc中，jing项目会尽可能保证默认的配置值在主线版本的变更中维持不变，修改默认固定值的做法
应该被用户自行fork的版本来进行维护

## 环境配置

环境配置主要依赖于系统的环境变量和启动参数中传递的properties选项，在jing项目中，他们只作为系统提供的特定环境信息被使用，而
不作为配置文件中的一部分，开发者应该自己决定，将配置项放在环境变量，启动参数，还是配置文件中，读取，解析并加载到特定的组件中

## 配置文件

jing项目的配置值第一公民只有字符串类型，以及字符串类型的数组，或是配置项的嵌套，整个配置文件呈现一个map的形式

配置文件主要依赖于通过SPI加载ConfigurationFacade的实现类
``` java
public interface ConfigurationFacade {
    String item(String key);

    List<String> itemList(String key);

    Map<String, String> itemMap(String key);
}
```

默认提供的ConfigurationFacade实现支持三种类型的配置文件：json，properties和toml，且需要明确注意，配置文件的语法并不是完整的json，properties，toml的语法合集
而是他们的一部分的特定功能子集，不遵循于某个特定的版本，一切按照jing文档中的描述为准，一个完备的解析器会带来巨大的额外工作量，以及引入一些非预期内的配置转化的错误

对于字符串的格式要求是，只允许字母（a-z, A-Z）、数字（0-9）、下划线（_）、连字符（-），不允许出现引号，空格，或其它的特殊字符

文件名后缀为.json时，jing的默认配置加载会以JSON格式完成对配置文件的解析，具体的格式规范请参考 https://www.json.org/json-en.html
对于json文件，key固定为字符串类型，value固定为字符串类型，或者是[]包裹的多个字符串类型，或者是{}包裹的符合上述两个条件的json键值对的对象
json中的null会被直接解析为不存在的配置项
json中的string，无论是key还是value，都可以被规范指定的escape转义支持
整个配置文件必须是一个由{}包裹的合法json对象体，不能是裸key value字符串

文件后缀名为.properties时，jing的默认配置加载会以PROPERTIES格式完成对配置文件的解析，properties有各种不同类型的style，在jing项目的默认配置解析中，对其进行了进一步的约束以防止错误的发生
对于properties文件，采取按行解析的策略，以#开头的是注释行，整行内容都会被直接drop掉
对于数据行，均采用k=v的形式进行键值对的描述，k和v前后的空格会被trim掉，如果需要填充空格，可以使用转义
支持对于k和v的转义处理，转义会处理以下情况：
backslash=\\                  # 反斜杠 \
period=\.                     # 句号 . 这个是为了字面值的句号
comma=\,                      # 逗号 ，在这个是为了字面值的逗号
equals=\=                     # 等号 =
space=\                       # 空格
tab=\t                        # 制表符
newline=\n                    # 换行符
carriage-return=\r            # 回车符
以及\u开头的unicode字符转义形式，不支持通过行尾的反斜杠进行续行转义，所有的key和value都必须在同一行内完成，以降低维护难度
key和value的分割只能使用等号，有一些规范支持使用冒号，或是空格来对键值对进行分割，但在jing项目中不支持这些其它的形式，语义明确是我们的首要目的
如果value字符串以`[`进行开头，以`]`进行结尾，那么这个value会被判定为一个数组类型，其中的所有数据项都是string类型，使用逗号进行字符串的分割

文件后缀名为.toml时，jing的默认配置加载会以TOML格式完成对配置文件的解析，具体的格式规范请参考 https://toml.io/en/v1.0.0
对于toml文件，采取按行解析的策略，对于以#开头的是注释行，整行内容都会被直接drop掉
toml的key只支持bare keys的模式，可以用dotted keys来连接bare keys，但是不允许quoted keys的形式，也就是keys里面不能用单引号或者双引号，也不允许出现任何的空格
字符串的值规定必须以双引号形式提供，支持
\b         - backspace       (U+0008)
\t         - tab             (U+0009)
\n         - linefeed        (U+000A)
\f         - form feed       (U+000C)
\r         - carriage return (U+000D)
\"         - quote           (U+0022)
\\         - backslash       (U+005C)
\uXXXX     - unicode         (U+XXXX)
\UXXXXXXXX - unicode         (U+XXXXXXXX) 形式的转义
字符串只允许在单行内完成书写，不允许出现多行字符串
字符串数组是以[]包裹的多个字符串
支持table headers，用.进行分割，之后的key value都属于该分支之下，table header严格使用[]进行分割，是多个key用.进行拼接的结果
其它的toml语法，比如[[]]之类的，都不支持



