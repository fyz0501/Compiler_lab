package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.symtab.SymbolTableEntry;

import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    class TokenType{
        public Token token;
        public SourceCodeType type;

        public TokenType(Token token) {
            this.token = token;
        }

        public TokenType(Token token, SourceCodeType type) {
            this.token = token;
            this.type = type;
        }

        public TokenType() {
        }

    }

    /**
     * semantic
     * 语义分析栈
     */
    Stack<TokenType> semantics = new Stack<>();

    SymbolTable table;

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
        // 看不懂指导书的我对着这里发愣了好久
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        TokenType[] popAttribute = new TokenType[10];
        // popAttribute数组用于存放语义栈中弹出的符号属性
        for(int i = 0;i<production.body().toArray().length;i++) {
            popAttribute[i] = semantics.pop();
        }
        switch (production.index()){
            //产生式为S -> D id时
            case 4:
                //在产生式S -> D id中，先pop了id的属性，再pop了类型D的属性，此处将类型D的属性压入栈
                semantics.push(popAttribute[1]);
                //更改符号表，将id的具体名字传入表中进行查找，并更改相应的type
                SymbolTableEntry entry = table.get(popAttribute[0].token.getText());
                entry.setType(popAttribute[0].type);
                break;
            //产生式为D -> int时
            case 5:
                semantics.push(new TokenType(Token.simple("int"),SourceCodeType.Int));

                break;
            //其他产生式压入空记录
            default:
                semantics.push(new TokenType());
        }

    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作
//        String tokenType = currentToken.getKindId();
//        //token的type为“id”时，需要将具体的id压入栈中
//        if(tokenType.equals("id")){
//            semantics.push(currentToken.getText());
//        }
//        else{
//            System.out.println(tokenType);
//            semantics.push(tokenType);
//        }
        TokenType tokenType;
        if(currentToken.getKindId().equals("id")){
            tokenType = new TokenType(currentToken, SourceCodeType.Int);
        }
        else{
            tokenType = new TokenType(currentToken);
        }
        semantics.push(tokenType);
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        this.table = table;
    }
}

