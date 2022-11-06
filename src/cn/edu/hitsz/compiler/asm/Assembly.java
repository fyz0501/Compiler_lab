package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 200110501符悦泽
 * @create 2022/11/3
 */
public class Assembly {
    public static Assembly createAdd(IRVariable result, IRValue lhs, IRValue rhs) {
        return new Assembly(AssemblyKind.add, result, List.of(lhs, rhs));
    }

    public static Assembly createAddi(IRVariable result, IRValue lhs, IRValue rhs) {
        return new Assembly(AssemblyKind.addi, result, List.of(lhs, rhs));
    }

    public static Assembly createSub(IRVariable result, IRValue lhs, IRValue rhs) {
        return new Assembly(AssemblyKind.sub, result, List.of(lhs, rhs));
    }

    public static Assembly createMul(IRVariable result, IRValue lhs, IRValue rhs) {
        return new Assembly(AssemblyKind.mul, result, List.of(lhs, rhs));
    }

    public static Assembly createMv(IRVariable result, IRValue from) {
        return new Assembly(AssemblyKind.mv, result, List.of(from));
    }

    public static Assembly createLi(IRVariable result, IRImmediate imm) {
        return new Assembly(AssemblyKind.li, result, List.of(imm));
    }


    //============================== 不同种类 IR 的参数 getter ==============================
    public AssemblyKind getKind() {
        return kind;
    }

    //============================== 基础设施 ==============================
    @Override
    public String toString() {
        final var kindString = kind.toString();
        final var resultString = result == null ? "" : result.toString();
        final var operandsString = operands.stream().map(Objects::toString).collect(Collectors.joining(", "));
        return "\t%s  %s, %s".formatted(kindString, resultString, operandsString);
    }

    public List<IRValue> getOperands() {
        return Collections.unmodifiableList(operands);
    }

    private Assembly(AssemblyKind kind, IRVariable result, List<IRValue> operands) {
        this.kind = kind;
        this.result = result;
        this.operands = operands;
    }

    private final AssemblyKind kind;
    private final IRVariable result;
    private final List<IRValue> operands;

}
