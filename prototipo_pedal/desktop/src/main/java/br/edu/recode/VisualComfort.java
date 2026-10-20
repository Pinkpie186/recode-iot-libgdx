package br.edu.recode;

/** Preferencia local para reduzir os movimentos decorativos do cenario. */
final class VisualComfort {
    private static boolean softEffects;

    private VisualComfort() { }

    static boolean softEffects() { return softEffects; }
    static void toggle() { softEffects = !softEffects; }
    static float ambientTime(float time) { return softEffects ? 0f : time; }
}
