package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    Stack<IRValue> irStack = new Stack<>();
    List<Instruction> instructionList = new ArrayList<>();
    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        String tokenType = currentToken.getKindId();
        switch (tokenType){
            case "id":
                irStack.push(IRVariable.named(currentToken.getText()));
                break;
            case "IntConst":
                irStack.push(IRImmediate.of(Integer.parseInt(currentToken.getText())));
                break;
            default:
                irStack.push(IRVariable.named((tokenType)));
                break;
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
        IRValue[] popAttribute = new IRValue[10];
        // popAttribute数组用于存放语义栈中弹出的符号属性
        for(int i = 0;i<production.body().toArray().length;i++) {
            popAttribute[i] = irStack.pop();
        }
        switch (production.index()){
            case 6:
                instructionList.add(Instruction.createMov((IRVariable)popAttribute[2],popAttribute[0]));
                irStack.push(IRVariable.named(""));
                break;
            case 7:
                instructionList.add(Instruction.createRet(popAttribute[0]));
                irStack.push(IRVariable.named(""));
                break;
            case 8:
                IRVariable result8 = IRVariable.temp();
                instructionList.add(Instruction.createAdd(result8, popAttribute[2], popAttribute[0]));
                irStack.push(result8);
                break;
            case 9:
                IRVariable result9 = IRVariable.temp();
                instructionList.add(Instruction.createSub(result9, popAttribute[2], popAttribute[0]));
                irStack.push(result9);
                break;
            case 11:
                IRVariable result11 = IRVariable.temp();
                instructionList.add(Instruction.createMul(result11,popAttribute[2],popAttribute[0]));
                irStack.push(result11);
                break;
            case 13:
                System.out.println(popAttribute[1]);
                irStack.push(popAttribute[1]);
                break;
            case 1,5,10,12,14,15:
                irStack.push(popAttribute[0]);
                break;
            default:
                irStack.push(IRVariable.named(""));
                break;
        }
    }

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
        // 假装做了什么
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        // 假装做了什么
    }

    public List<Instruction> getIR() {
        // TODO
        return instructionList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

