package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.utils.FileUtils;
import cn.edu.hitsz.compiler.utils.IREmulator;
import java.util.*;
import java.util.stream.StreamSupport;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    /**
     * BinaryMap
     * 维护一个双射变量-寄存器分配表
     **/
    public class BinaryMap<K, V> {
        private final Map<K, V> KVmap = new HashMap<>();
        private final Map<V, K> VKmap = new HashMap<>();

        public void removeByKey(K key) {
            VKmap.remove(KVmap.remove(key));
        }

        public void removeByValue(V value) {
            KVmap.remove(VKmap.remove(value));

        }

        public void put(K key,V value){
            KVmap.put(key, value);
            VKmap.put(value, key);
        }

        public boolean containsKey(K key) {
            return KVmap.containsKey(key);
        }

        public boolean containsValue(V value) {
            return VKmap.containsKey(value);
        }

        public void replace(K key, V value) {
            // 对于双射关系, 将会删除交叉项
            if(containsKey(key)) {
                removeByKey(key);
                removeByValue(value);
                KVmap.put(key, value);
                VKmap.put(value, key);
            }
            else{
                KVmap.put(key, value);
                VKmap.put(value, key);
            }
        }

        public V getByKey(K key) {
            return KVmap.get(key);
        }

        public K getByValue(V value) {
            return VKmap.get(value);
        }
    }

    /**
     * usedTimes
     * 维护一个记录变量后续引用次数的Map，可以根据变量名称查找其后续引用次数
     */
    private final Map<IRVariable, Integer> usedTimes = new HashMap<>();

    private void cite(IRVariable v){
        if(usedTimes.containsKey(v)){            //如果该变量已经在Map中，则不需要初始化
            usedTimes.put(v, usedTimes.get(v)+1);//将该变量对应的引用次数+1
        }
        else{
            usedTimes.put(v, 1);                //将该变量加到Map中，引用次数初始化为1
        }
    }

    private void deCite(IRVariable v){  //使用该变量后，后续引用次数-1
        if(usedTimes.containsKey(v) && usedTimes.get(v)>0){
            usedTimes.put(v, usedTimes.get(v)-1);//将该变量对应的引用次数-1
        }
    }

    /**
     * regList
     * 定义一个空闲寄存器集合
     */
    Set<String> regList = new HashSet<>(){
        {
            for(int i = 0;i < 7;i++){
                add("t"+i);
            }
        }
    };
    private int regNum = 7;//寄存器的数量为7
    private int usedNum = 0;//初始化已使用的寄存器数量为0

    /**
     * afterInstructions
     * 预处理后的中间代码
     */
    List<Instruction> afterInstructions = new ArrayList<>();

    /**
     * regVars
     * 寄存器与变量的双射Map
     **/
    private final BinaryMap<String, IRVariable> regVars = new BinaryMap<>();

    /**
     * assemblyInstruction
     * 生成的汇编指令
     */
    List<Assembly> assemblyInstruction = new ArrayList<>();

    /**
     *
     * disableReg
     * 查看该某个寄存器是否可用，防止rhs抢占res、lhs寄存器的情况
     */
    private boolean disableReg(String[] regs, String regObj){
        for(int i = 0;i < regs.length;i++){
            if(regObj.equals(regs[i])){
                return false;
            }
        }
        return true;
    }

    /**
     * allocation
     * 寄存器分配函数
     */
    private  void allocation(IRVariable v,String ... regs){
        if(regVars.containsValue(v)){      //如果双向Map中已经存在变量v，则不用再分配寄存器
            return;
        }
        else if(usedNum < regNum){
            String reg = regList.iterator().next();
            regList.remove(reg);    //将分配的寄存器从空闲寄存器集合中移除
            regVars.put(reg, v);
            usedNum++;              //已使用的寄存器数量增加
        }
        else {                       //寄存器已分配满
            int leastTimes = 10000;
            String regName = null;
            for (String reg : regVars.KVmap.keySet()) {         //遍历查找regVars中所有的寄存器reg
                int time = usedTimes.get(regVars.getByKey(reg)); //根据寄存器的名字在regVars中找到变量，根据变量再去usedTimes中找到后续引用次数最少的变量所存放的寄存器
                if (time < leastTimes && disableReg(regs, reg)) {   //此处不需要将该reg设为空闲寄存器，只需要找到引用次数最少的变量所放的寄存器(不能是被禁用的寄存器)，将该变量替换为新变量v即可
                    if(time == 0){          //如果后续不再引用，则直接将该变量移出寄存器
                        regList.add(reg);   //将该reg设为空闲寄存器
                        regName = reg;      //需要替换的寄存器设为该reg
                        break;
                    }
                    else {
                        leastTimes = time;
                        regName = reg;
                    }
                }
            }
            regVars.replace(regName, v);
        }
    }

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用(?)
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        //预处理
        boolean retFlag = false;
        for(Instruction instruction:originInstructions){
            if(retFlag){
                break;
            }
            InstructionKind kind = instruction.getKind();
            IRValue lhs;
            IRValue rhs;
            IRVariable res;
            switch (kind){
                case RET:
                    var ret = instruction.getReturnValue();
                    cite((IRVariable) ret);
                    afterInstructions.add(Instruction.createRet(ret));
                    retFlag = true; //读到RET指令，将retFlag置为true表示舍弃后续指令
                    break;
                case ADD:
                    res = instruction.getResult();
                    lhs = instruction.getLHS();
                    rhs = instruction.getRHS();
                    cite(res);
                    if(lhs instanceof IRImmediate l && rhs instanceof  IRImmediate r) {
                        afterInstructions.add(Instruction.createMov(res, IRImmediate.of(r.getValue() + l.getValue())));
                    }
                    else if(lhs instanceof IRImmediate l && rhs instanceof IRVariable r){
                        afterInstructions.add(Instruction.createAdd(res, r, l));
                        cite(r);
                    }
                    else {  //不需要调整时只需要将原Instruction添加到afterInstructions中,并添加相应的引用次数
                        afterInstructions.add(instruction);
                        if(lhs instanceof IRVariable l){
                            cite(l);
                        }
                        if(rhs instanceof IRVariable r){
                            cite(r);
                        }
                    }
                    break;
                case SUB:
                    res = instruction.getResult();
                    lhs = instruction.getLHS();
                    rhs = instruction.getRHS();
                    cite(res);
                    if(lhs instanceof IRImmediate l && rhs instanceof  IRImmediate r) {
                        afterInstructions.add(Instruction.createMov(res, IRImmediate.of(l.getValue() - r.getValue())));
                    }
                    else if(lhs instanceof IRImmediate l && rhs instanceof IRVariable r){
                        IRVariable temp_l = IRVariable.temp();
                        afterInstructions.add(Instruction.createMov(temp_l, IRImmediate.of(l.getValue())));//用临时变量替换立即数
                        afterInstructions.add(Instruction.createSub(res, temp_l, r));
                        cite(temp_l);
                        cite(r);
                    }
                    else {
                        afterInstructions.add(instruction);
                        if(lhs instanceof IRVariable l){
                            cite(l);
                        }
                        if(rhs instanceof IRVariable r){
                            cite(r);
                        }
                    }
                    break;
                case MUL:
                    /** riscv32中不存在立即数乘法，因此将所有含立即数的MUL Instruction进行替代 **/
                    res = instruction.getResult();
                    lhs = instruction.getLHS();
                    rhs = instruction.getRHS();
                    cite(res);
                    if(lhs instanceof IRImmediate l && rhs instanceof  IRImmediate r) {
                        IRVariable temp_l = IRVariable.temp();
                        IRVariable temp_r = IRVariable.temp();
                        afterInstructions.add(Instruction.createMov(temp_l, IRImmediate.of(l.getValue())));//用临时变量替换立即数
                        afterInstructions.add(Instruction.createMov(temp_r, IRImmediate.of(r.getValue())));
                        afterInstructions.add(Instruction.createMul(res, temp_l, temp_r));
                        cite(temp_l);
                        cite(temp_r);
                    }
                    else if(lhs instanceof IRImmediate l && rhs instanceof IRVariable r){
                        IRVariable temp_l = IRVariable.temp();
                        afterInstructions.add(Instruction.createMov(temp_l, IRImmediate.of(l.getValue())));//用临时变量替换立即数
                        afterInstructions.add(Instruction.createMul(res, temp_l, r));
                        cite(temp_l);
                        cite(r);
                    }
                    else if(lhs instanceof IRVariable l && rhs instanceof IRImmediate r){
                        IRVariable temp_r = IRVariable.temp();
                        afterInstructions.add(Instruction.createMov(temp_r, IRImmediate.of(r.getValue())));//用临时变量替换立即数
                        afterInstructions.add(Instruction.createMul(res, l, temp_r));
                        cite(l);
                        cite(temp_r);
                    }
                    else {
                        cite((IRVariable) lhs); //排除其他情况后，lhs和rhs只能为IRVariable
                        cite((IRVariable) rhs);
                        afterInstructions.add(instruction);
                    }
                    break;
                case MOV:
                    ret = instruction.getResult();
                    var from = instruction.getFrom();
                    cite((IRVariable) ret);//mov会移动到变量中去，直接类型转换成IRVariable
                    if(from instanceof IRVariable f){
                        cite(f);
                    }
                    afterInstructions.add(Instruction.createMov((IRVariable) ret, from));
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成

        // 寄存器分配
        for(Instruction instruction: afterInstructions){
            InstructionKind kind = instruction.getKind();
            switch (kind){
                case ADD, SUB, MUL:
                    var lhs = instruction.getLHS();
                    var rhs = instruction.getRHS();
                    var res = instruction.getResult();
                    allocation(res);
                    deCite(res);
                    allocation((IRVariable) lhs, regVars.getByValue(res));//由于进行了预处理，lhs一定是变量，但是仍需要对rhs进行判断
                    deCite((IRVariable) lhs);   //后续引用次数减少
                    if(rhs instanceof IRVariable r){
                        allocation(r,regVars.getByValue(res),regVars.getByValue((IRVariable)lhs));
                        deCite(r);  //r为变量类型，减少其后续引用次数
                        switch (kind){
                            case ADD -> {
                                assemblyInstruction.add(Assembly.createAdd(IRVariable.named(regVars.getByValue(res)), IRVariable.named(regVars.getByValue((IRVariable) lhs)), IRVariable.named(regVars.getByValue((IRVariable) rhs))));
                            }
                            case SUB -> {
                                assemblyInstruction.add(Assembly.createSub(IRVariable.named(regVars.getByValue(res)), IRVariable.named(regVars.getByValue((IRVariable) lhs)), IRVariable.named(regVars.getByValue((IRVariable) rhs))));
                            }
                            case MUL -> {
                                assemblyInstruction.add(Assembly.createMul(IRVariable.named(regVars.getByValue(res)), IRVariable.named(regVars.getByValue((IRVariable) lhs)), IRVariable.named(regVars.getByValue((IRVariable) rhs))));
                            }
                        }
                    }
                    else{       //addi指令
                        assemblyInstruction.add(Assembly.createAddi(IRVariable.named(regVars.getByValue(res)), IRVariable.named(regVars.getByValue((IRVariable) lhs)), rhs));
                    }
                    break;
                case MOV:
                    var result = instruction.getResult();
                    allocation(result);

                    var from = instruction.getFrom();
                    if(from instanceof IRVariable f){   //from为变量，创建mv指令
                        allocation(f);
                        deCite(f);      // 变量需要deCite
                        assemblyInstruction.add(Assembly.createMv(IRVariable.named(regVars.getByValue(result)), IRVariable.named(regVars.getByValue(f))));
                    }
                    else{  // from为立即数，创建li指令
                        assemblyInstruction.add(Assembly.createLi(IRVariable.named(regVars.getByValue(result)), (IRImmediate) from));
                    }
                    break;
                case RET:
                    var ret = instruction.getReturnValue();
                    allocation((IRVariable) ret);
                    deCite((IRVariable) ret);//ret指令的参数为变量
                    assemblyInstruction.add(Assembly.createMv(IRVariable.named("a0"), IRVariable.named(regVars.getByValue((IRVariable) ret))));
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
//        System.out.println(assemblyInstruction);
        // TODO: 输出汇编代码到文件
//        ArrayList<String> text = new ArrayList<String>();
//        text.add(".text\n");
//        FileUtils.writeLines(path, text);\
        List<String> text = assemblyInstruction.stream().map(Assembly::toString).toList();
        ArrayList<String> strings = new ArrayList<>(text);
        strings.add(0, ".text");

        FileUtils.writeLines(path,strings
                );
    }
}
