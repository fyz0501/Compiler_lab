package cn.edu.hitsz.compiler.asm;

/**
 * @author 200110501符悦泽
 * @create 2022/11/3
 */
public enum AssemblyKind {
    li, add, addi, sub, mul, mv;

    /**
     * @return asm 是否是二元的 (有返回值, 有两个参数)
     */
    public boolean isBinary() {
        return this != mv && this != li;
    }

    /**
     * @return asm 是否是一元的 (有返回值, 有一个参数)
     */
    public boolean isUnary() {
        return this == mv || this == li;
    }
}
