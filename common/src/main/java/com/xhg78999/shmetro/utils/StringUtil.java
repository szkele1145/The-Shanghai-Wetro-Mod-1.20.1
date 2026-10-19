package com.xhg78999.shmetro.utils;

import java.util.regex.Pattern;

/**
 * 站名拆分工具。
 *
 * <p>MTR 的站名用 {@code ||} 分隔主名与注释、用 {@code |} 分隔中文名与英文名，
 * 形如 {@code "人民广场|People's Square||换乘 1 号线"}。</p>
 *
 * <p>热路径说明：{@link #getCjkMain} / {@link #getEngMain} 会在方块实体渲染器
 * （{@code RenderStationInfoSingle} / {@code RenderStationInfoDouble} /
 * {@code RenderStationNameWall}）里每帧被调用。原实现是
 * {@code name.split("\\|\\|")} —— JDK 的 {@code String.split(regex, limit)} 内部为
 * {@code Pattern.compile(regex).split(this, limit)}，且其免编译 fastpath 只覆盖
 * 「单字符非元字符」与「两字符反斜杠转义」，{@code "\\|\\|"} 有 4 个字符，
 * <b>每次调用都会重新解析并编译正则、分配一个 Pattern 对象</b>。
 * 这里把它提为 {@code static final Pattern}，把每帧的编译降为一次性编译。</p>
 *
 * <p>{@code Pattern.split(CharSequence)} 与 {@code String.split(String)} 的语义完全等价
 * （两者都委托 {@code split(input, 0)}），因此本改动不改变任何可观察行为，
 * 包括各种退化输入下的异常回退路径。{@code "\\|"} 本就命中 String.split 的
 * 免编译 fastpath，保持原样不动。</p>
 */
public class StringUtil {
    /** 主名 / 注释分隔符 {@code ||} 的预编译正则。Pattern 是线程安全的。 */
    private static final Pattern ANNOTATION_SEPARATOR = Pattern.compile("\\|\\|");

    public static String getCjkMain(String name) {
        try {
            return ANNOTATION_SEPARATOR.split(name)[0].split("\\|")[0];
        }
        catch (Exception e) {
            return name;
        }
    }

    public static String getEngMain(String name) {
        try {
            return ANNOTATION_SEPARATOR.split(name)[0].split("\\|")[1];
        }
        catch (Exception e) {
            return "";
        }
    }

    public static String getAnnotation(String name) {
        try {
            return ANNOTATION_SEPARATOR.split(name)[1];
        }
        catch (Exception e) {
            return "";
        }
    }

    /**
     * 一次性解析出中文主名与英文主名。
     *
     * <p>热路径说明：站名渲染器（{@code RenderStationInfoSingle} /
     * {@code RenderStationInfoDouble} / {@code RenderStationNameWall}）每帧都会
     * 对<b>同一个</b>站名字符串先后调用 {@link #getCjkMain} 和 {@link #getEngMain}，
     * 两个方法各自独立做一次 {@code split("\\|\\|")} + 一次 {@code split("\\|")}，
     * 等于把同一份解析工作做了两遍。调用方若只需要中英文两段，用本方法
     * 一次解析即可拿到两个结果。</p>
     *
     * <p>行为与逐个调用 {@code getCjkMain} / {@code getEngMain} <b>完全一致</b>：
     * 两者的异常回退分别是「原名」与「空串」，本方法保持同样的回退值。
     * 本方法只做解析、不改变任何可观察结果。</p>
     *
     * @param name 原始站名，形如 {@code "人民广场|People's Square||换乘 1 号线"}
     * @return 长度为 2 的数组：[0] = 中文主名，[1] = 英文主名
     */
    public static String[] getCjkAndEngMain(String name) {
        try {
            String main = ANNOTATION_SEPARATOR.split(name)[0];
            String[] cjkAndEng = main.split("\\|");
            return new String[]{cjkAndEng[0], cjkAndEng[1]};
        }
        catch (Exception e) {
            return new String[]{name, ""};
        }
    }
}
