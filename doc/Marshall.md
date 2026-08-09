Marshall方面的设计思路

核心是编译期的元数据提供，从而实现多种不同序列化格式的支持

序列化和反序列化注定无法适配java中多样化的动态类型，因此转向一种，面向接口的静态匹配机制

预计算的类型只包含基础类型，数组类型，以及Collection Map在标准库java.util中相关的接口子类匹配，其余的类型统一使用object进行匹配

类型的转化器强制要求，其中一种是用户的任意类型，另一种是原生支持的内置类型，实现从不支持的类型到支持的类型的转化

内置支持的每一种序列化格式有特定的spec，只支持部分常见的格式即可，如果有需求，可以让用户自己修改源码添加相关支持

# spec

这里是marshall实现层面上的spec介绍

marshall分为两个部分，第一部分是编译期基于注解处理器的元数据生成，生成的元数据通过注册SPI的方式，可以在运行时检索，第二部分是基于
这些元数据而进行特定格式的序列化和反序列化，包含json和cbor的实现

编译期的元数据生成主要想要解决以下几个问题：

1. 在不使用反射的情况下，如何获取一个对象中的某个字段的信息
2. 在不使用反射的情况下，如何给一个对象中的某个字段赋值，并构造对象
3. 将一部分可在编译期计算出来的信息，从运行期移交到编译期执行，从而获得更好的不可变性

基于这些需求，做了以下的几点设计：

对于需要提供元数据的类，用Marshallable注解进行标记，可以通过MarshallAttr注解给字段添加额外的标注信息，注解处理器会根据类中的字段
以及这两个注解提供的信息，来决定元数据的生成

针对序列化和反序列化的常见，提供了MarshallWriter和MarshallReader两个接口，分别用于将对象的字段数据写入到writeBuffer，以及从readBuffer中读取对象字段的数据

针对自定义的序列化与反序列化方式，提供了MarshallTransformer，通过注解处理器，可以实现自动的注册，通过在options中指定要应用的transformer，可以影响序列化和反序列化的解析行为

MarshallProcessor和MarshallTransformerProcessor都共用基础processor的基本方法，都是类型系统的简单性优先，这种设计会损失一些
运行时的灵活性，对于一些复杂的类型没有办法提供支持，但整体会更容易理解和维护

Marshall的限制主要体现在以下几点：

java可以写出非常复杂的泛型类型，产生很深的相互嵌套，而序列化场景中，大部分的泛型相关需求是支持jdk内置的List，Map这一类容器类型，为了让
整体框架的设计更简化，Marshall只保留当前层级的至多2个泛型类型，比如Map<Integer, String>
字段，可以通过MarshallInfo.firstGenericType ()
与MarshallInfo.secondGenericType ()拿到Integer和String类型，对于嵌套（比如List<List<String>>）或更多泛型参数（比如Custom<A,
B, C>） 的情况，是不允许通过编译期检查的

transformer本身是一个泛型接口，实现任意的a类型到b类型的互相转化，在格式无关的前提下，marshall要求custom和builtin泛型都必须是
非数组（transformer通常可以被实现为对数组中的每个元素进行适配，因此无须针对数组编写），无泛型参数（因为类型擦除的原因，无法做精准的匹配，因此没有支持的必要）的简单类型
在实际的使用中，框架一般还会添加一些额外的限制要求，比如在jing项目的json实现中，要求builtin类型必须为JsonPrimitiveType接口或其实现类，这类要求可以进一步降低transformer的误用率，使得整体设计更加健壮

## json

json的核心是key value的相互映射，是目前最通用的一种序列化格式，因此直接内置支持

json内置的数据类型主要是以下几种：

1. null类型，代表空值
2. bool类型，只有true和false两个选项
3. 整数类型，范围没有特定限制
4. 浮点数类型，范围没有特定限制
5. 字符串类型，其中可能有转义序列
6. 数组类型，其中可能包含其他的任何类型
7. 对象类型，用键值对表示

json中的键总是字符串类型

对于null类型，在java中不需要有对应的类型，因为它只需要表示空值即可 对于bool类型，对应java中的boolean和Boolean的情况
对于整数类型，从byte到long的整数类型，以及他们的包装类，都可以进行对应的转化，在这里统一使用long进行处理
对于浮点数类型，用float和double以及其包装类进行表示，这两者因为精度不同因此不能混淆 对于字符串类型，可以用String和CharSequence来进行表示
数组类型是对于其他类型的数组封装，对应java中的数组或Collection类型 对象类型直接对应java中的对象或Map类型

### json serialization

序列化的入口，支持以下几种类型

1. 实现了marshall的class或record实例，可序列化为json的对象
2. Map<K,V>实例，要求K必须为String或CharSequence类型，V是可支持的值类型，可序列化为json的对象
3. Collection<T>实例，要求T是可支持的值类型，可序列化为json的数组
4. T[]实例，要求T是可支持的值类型，可序列化为json的数组

按照序列化时使用的优先级，来定义以下为可支持的值类型：

1. 内置支持所有的primitive type：byte boolean char short int long float double，其中byte short int long会被序列化为json整数数字，
   float和double会被序列化为json浮点数数字，boolean会被序列化为json布尔值，char会被序列化为只包含单个字符的json字符串，这些序列化行为不能被覆盖
2. 内置支持所有的primitive wrapper type，序列化行为在为非null情况下和primitive type完全保持一致，在为null时会写入json的null值
3. 内置支持所有primitive type array和primitive wrapper type array，数组值的序列化行为如上，用json数组的形式对其进行封装，在为null时会写入json的null值
4. 内置支持String和CharSequence作为json字符串类型进行写入，在为null时会写入json的null值
5. 内置支持JsonPrimitiveType和其实现类JsonBoolType,JsonNumberType,JsonStrType，作为json布尔值，json数字值，json字符串值进行写入
6. 根据用户提供的MarshallTransformer判断，是否能够将期望类型转化为JsonPrimitiveType基础类型，如果可以则转化后写入（以上5️点是固定规范，其类型无法被Transformer覆盖）
7. 内置支持所有的枚举类，用json字符串类型，在非Marshallable的情况下，写入枚举项的name，在Marshallable的情况下，写入枚举项的mappedName
8. 最后判断类型是否为Marshallable，如果成立，按照json对象的形式写入

### json deserialization



