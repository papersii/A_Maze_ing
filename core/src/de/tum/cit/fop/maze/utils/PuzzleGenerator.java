package de.tum.cit.fop.maze.utils;

import de.tum.cit.fop.maze.model.Puzzle;
import java.util.Random;

/**
 * 谜题生成器 (Puzzle Generator)
 * 
 * 为谜题宝箱生成随机谜题。
 * 支持数学题、逻辑题和选择题。
 */
public class PuzzleGenerator {

    // ========== 谜题题库 ==========

    private static final String[][] MATH_QUESTIONS = {
            { "3 + 5 = ?", "8" },
            { "12 - 7 = ?", "5" },
            { "4 × 3 = ?", "12" },
            { "15 ÷ 3 = ?", "5" },
            { "7 + 8 = ?", "15" },
            { "20 - 9 = ?", "11" },
            { "6 × 6 = ?", "36" },
            { "24 ÷ 4 = ?", "6" },
            { "9 + 6 = ?", "15" },
            { "18 - 9 = ?", "9" }
    };

    private static final Object[][] LOGIC_QUESTIONS = {
            { "下列哪个是哺乳动物？", "A", new String[] { "A. 鲸鱼", "B. 蜥蜴", "C. 鲨鱼", "D. 蝴蝶" } },
            { "一年有多少个月？", "B", new String[] { "A. 10", "B. 12", "C. 14", "D. 16" } },
            { "地球是什么形状？", "C", new String[] { "A. 正方形", "B. 三角形", "C. 球形", "D. 圆柱形" } },
            { "太阳从哪个方向升起？", "A", new String[] { "A. 东方", "B. 西方", "C. 南方", "D. 北方" } },
            { "水的化学式是？", "B", new String[] { "A. CO2", "B. H2O", "C. O2", "D. NaCl" } },
            { "1+1=?", "C", new String[] { "A. 1", "B. 0", "C. 2", "D. 3" } },
            { "哪种颜色是由红色和蓝色混合而成？", "D", new String[] { "A. 绿色", "B. 黄色", "C. 橙色", "D. 紫色" } },
            { "一周有多少天？", "A", new String[] { "A. 7", "B. 5", "C. 10", "D. 6" } }
    };

    // ========== 生成方法 ==========

    /**
     * 生成随机谜题
     * 
     * @param random 随机数生成器
     * @return 随机谜题
     */
    public static Puzzle generateRandom(Random random) {
        // 50% 数学题, 50% 逻辑选择题
        if (random.nextBoolean()) {
            return generateMathPuzzle(random);
        } else {
            return generateLogicPuzzle(random);
        }
    }

    /**
     * 生成数学题
     */
    public static Puzzle generateMathPuzzle(Random random) {
        int index = random.nextInt(MATH_QUESTIONS.length);
        String[] qa = MATH_QUESTIONS[index];
        return new Puzzle(qa[0], qa[1], Puzzle.PuzzleType.MATH);
    }

    /**
     * 生成逻辑选择题
     */
    public static Puzzle generateLogicPuzzle(Random random) {
        int index = random.nextInt(LOGIC_QUESTIONS.length);
        Object[] qa = LOGIC_QUESTIONS[index];
        String question = (String) qa[0];
        String answer = (String) qa[1];
        String[] options = (String[]) qa[2];
        return new Puzzle(question, answer, options, Puzzle.PuzzleType.LOGIC);
    }
}
