# GROUP: G7C


### Evaluation

| Name          | Number    | Grade | Contribution |
| ------------- | --------- | ----- | ------------ |
| Caio Nogueira | 201806218 | 20    | 25%          |
| Diogo Almeida | 201806630 | 20    | 25%          |
| Miguel Silva  | 201806388 | 20    | 25%          |
| Telmo Botelho | 201806821 | 20    | 25%          |

**Global Grade of The Project:** 19

 

### Summary

This tool parses .jmm code and converts it into an abstract syntax tree (AST), analyzing it for possible syntactic or semantic errors and then converting it into OLLIR code. This OLLIR code is then converted into an OLLIR class which is converted into JVM. The JVM code can then be executed.



### Dealing with syntactic errors

 Upon encountering a syntactic error, the parser adds it to the reports list (as a syntactic error) and exits immediately after.



### Semantic Analysis

The semantic analysis verifies everything that was required in checklist for CP2:

- **Expression Analysis**
  - Verifies if operations are of the same type (e.g. int + boolean gives an error);
  - Arithmetic operations can't use direct array access (e.g. array1 + array2);
  - Verifies if an array access is done in an array (e.g. 1[10] isn't allowed);
  - Verifies if an array index is, in fact, an integer (e.g. a[true] isn't allowed); 
  - Verifies if the assignee's value is equal to assigned's value (a_int = b_boolean isn't allowed);
  - Verifies if boolean operations (&&, < or !) only contain booleans;
  - Verifies if conditional expressions (if and while) result in a boolean;




- **Method Verification**
  - Verifies if a target's method exists, and it it contains a method (e.g. a.foo, checks it 'a' exists and if there is a method 'foo') 
    - In case it is the declared class's type (e.g. using this), if there isn't a declaration in the code and if it doesn't extend another class it returns an error. If it does extend another class then we just assume the method is from the super class.
  - In case the method isn't from the declared class, that is, is from the imported class, we assume it exists and assume the correct types (e.g. a = Foo.b(), if a is an integer, and Foo is an imported class, we assume the b method is static, that is doesn't have arguments and that it returns an integer).
  - If there is no way to know the type of the method (e.g. Foo.b(Foo.a()), if Foo is an imported class, we can't know what Foo.a() returns) we just assume if returns void in OLLIR (.V) which will fail in most cases later.
  - Verifies if the number of arguments in the invocation is the same as the parameters in the declaration;
  - Verifies if the type of the parameters is the same as the type of the arguments;



### Code Generation

 The .jmm code starts by being parsed into an abstract syntax tree (AST), which is used to generate OLLIR code. This code is later converted into an OLLIR class, which allows the JVM code to be generated and executed. There might be some issues if some problem occurs at a certain stage of the code (e.g. if the semantic analysis lets wrong code go through).



### Task Distribution

| Name          | Tasks                                                        |
| ------------- | ------------------------------------------------------------ |
| Caio Nogueira | AST (CP1), Semantic Analysis (CP2), Jasmin (CP2), Jasmin (CP3), Jasmin Optimizations (Final Delivery) |
| Diogo Almeida | AST (CP1), Semantic Analysis (CP2), OLLIR (CP2), OLLIR (CP3), OLLIR Optimizations (Final Delivery) |
| Miguel Silva  | AST (CP1), Jasmin (CP2), Jasmin (CP3), Jasmin Optimizations (Final Delivery) |
| Telmo Botelho | AST (CP1), OLLIR (CP2), OLLIR (CP3), OLLIR Optimizations (Final Delivery) |



### Pros

- The compiler contains some of the suggested optimizations: -o (Loop templates), -r (Register Allocation) for OLLIR and instruction selection (iconst vs sipush vs bipush vs ldc ; iinc vs iadd vs isub; if_icmpeq vs iflt vs ifge vs ifeq) for Jasmin;
- The compiler also contains some extra optimizations, on both OLLIR side and Jasmin:
  - OLLIR: Aux variable recycling and variable sanitization;
  - Jasmin: Not (Replaces condition branches with isub);
- OLLIR and Jasmin are well implemented and should be able to run code with complex expressions.



### Cons

- The compiler could have used more optimizations, like constant propagation;
- There might be some semantic errors that are accepted, since there are countless possibilities;
- The compiler does not support method overloading, and will fail when parsing code with 2 or more methods with the same name.

