# workflow

- Mixin 注入
- FeCA (入口点)
  - Rules.initializeImplClasses
    - 规则的实现类初始化
      - 注册 OnInitializeCallbackRegistry 回调事件 
  - 执行其它初始化
  - 执行 OnInitializeCallbackRegistry 回调