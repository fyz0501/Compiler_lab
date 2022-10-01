package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }
    private BufferedInputStream bufferedInputStream = null;
    private char[] readBuffer = new char[1000];
    //private List readBuffer = new ArrayList<String>();
    private FileReader bufferFr = null;


    /**
     * 维护一个token集合
     */
    private List<Token> tokenArray = new ArrayList<Token>();

    /**
     * 自动机的当前状态status
     */
    private int status = 0;

    /**
     * 指针pointer
     */
    private int pointer = 0;

    /**
     * 维护一个关键字列表keyWords
     */
    String[] keyWords = {"return","int"};


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) throws IOException {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        bufferFr = new FileReader(path);
        int num =  bufferFr.read(readBuffer);
        while(num != -1){
            num =  bufferFr.read(readBuffer);
        }
    }

    public boolean isLetter(char x){
        if((x>='A'&& x<='Z')||(x >='a'&& x <='z')){
            return true;
        }
        else{
            return false;
        }
    }

    public boolean isDigit(char x){
        if(x>='0'&& x<='9'){
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() throws IOException {
        // TODO: 自动机实现的词法分析过程

        for(pointer = 0;pointer<readBuffer.length;pointer++){
            /* clear */
            if(readBuffer[pointer]==' '||readBuffer[pointer]=='\n'||readBuffer[pointer]=='\t'){
                continue;
            }
            /* got a letter*/
            else if(isLetter(readBuffer[pointer])){
                StringBuffer stringTemp = new StringBuffer();
                while(isLetter(readBuffer[pointer])||isDigit(readBuffer[pointer])){
                    stringTemp.append(readBuffer[pointer]);
                    pointer++;
                }
                String token = stringTemp.toString();
                Token t = null;

                /* got a keyword */
                for(String keywords:keyWords){
                    if(keywords.equals(token)){
                        t = Token.simple(token);
                        tokenArray.add(t);
                        pointer--;
                        break;
                    }
                }

                /* got a id */
                if(t == null) {

                    /*judge whether should we add the id into symbolTable*/
                    if(!symbolTable.has(token)){
                        symbolTable.add(token);
                    }
                    tokenArray.add(Token.normal("id",token));
                    pointer--;
                }
            }

            /* got a digit */
            else if(isDigit(readBuffer[pointer])){
                StringBuffer stringTemp = new StringBuffer();
                while(isDigit(readBuffer[pointer])){
                    stringTemp.append(readBuffer[pointer]);
                    pointer++;
                }
                String numToken = stringTemp.toString();
                tokenArray.add(Token.normal("IntConst",numToken));
                pointer--;
            }
            /* got a '= , + - * / ( )‘ */
            else if(readBuffer[pointer]=='='||readBuffer[pointer]=='*' ||readBuffer[pointer]=='，'
                    ||readBuffer[pointer]=='+' ||readBuffer[pointer]=='-'
                    ||readBuffer[pointer]=='/'||readBuffer[pointer]=='(' ||readBuffer[pointer]==')'){
                String nn = String.valueOf(readBuffer[pointer]);
                Token zip = Token.simple(String.valueOf(readBuffer[pointer]));
                tokenArray.add(zip);
            }
            else if(readBuffer[pointer] == ';'){
                tokenArray.add(Token.simple("Semicolon"));
            }
        }
        // 为文件末尾加上EOF
        tokenArray.add(Token.eof());
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokenArray;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
                path,
                StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
