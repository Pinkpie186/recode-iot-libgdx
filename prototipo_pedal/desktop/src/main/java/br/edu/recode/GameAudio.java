package br.edu.recode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

/** Trilha de cada fase e efeitos curtos dos acontecimentos da campanha. */
final class GameAudio implements AutoCloseable {
    private final Music[] tracks = new Music[3];
    private Music menuTrack;
    private Sound repair;
    private Sound shield;
    private Sound hit;
    private Sound gameOver;
    private Sound navigate;
    private Sound confirm;
    private int currentPhase;
    private boolean playing;
    private boolean menuPlaying;

    GameAudio() {
        try {
            menuTrack = Gdx.audio.newMusic(Gdx.files.internal("audio/menu.wav"));
            menuTrack.setLooping(true);
            menuTrack.setVolume(0.22f);
            for (int i = 0; i < tracks.length; i++) {
                tracks[i] = Gdx.audio.newMusic(Gdx.files.internal("audio/fase_" + (i + 1) + ".wav"));
                tracks[i].setLooping(true);
                tracks[i].setVolume(i == 1 ? 0.22f : 0.26f);
            }
            repair = Gdx.audio.newSound(Gdx.files.internal("audio/reparo.wav"));
            shield = Gdx.audio.newSound(Gdx.files.internal("audio/escudo.wav"));
            hit = Gdx.audio.newSound(Gdx.files.internal("audio/impacto.wav"));
            gameOver = Gdx.audio.newSound(Gdx.files.internal("audio/fim_de_jogo.wav"));
            navigate = Gdx.audio.newSound(Gdx.files.internal("audio/navegar.wav"));
            confirm = Gdx.audio.newSound(Gdx.files.internal("audio/confirmar.wav"));
        } catch (RuntimeException error) {
            // Um computador sem dispositivo de audio ainda pode executar o jogo.
            Gdx.app.error("ReCode", "Audio indisponivel", error);
            close();
        }
    }

    void phase(int phase) {
        if (phase == currentPhase) return;
        if (currentPhase > 0 && tracks[currentPhase - 1] != null)
            tracks[currentPhase - 1].stop();
        currentPhase = phase;
        playing = false;
    }

    void playing(boolean active) {
        if (currentPhase < 1 || currentPhase > tracks.length || playing == active) return;
        Music track = tracks[currentPhase - 1];
        if (track == null) return;
        playing = active;
        if (active) track.play();
        else track.pause();
    }

    void menu(boolean active) {
        if (menuTrack == null || menuPlaying == active) return;
        menuPlaying = active;
        if (active) menuTrack.play();
        else menuTrack.pause();
    }

    void repair() { if (repair != null) repair.play(0.65f); }
    void shield() { if (shield != null) shield.play(0.72f); }
    void hit() { if (hit != null) hit.play(0.74f); }
    void navigate() { if (navigate != null) navigate.play(0.34f); }
    void confirm() { if (confirm != null) confirm.play(0.48f); }

    void gameOver() {
        playing(false);
        if (gameOver != null) gameOver.play(0.76f);
    }

    @Override public void close() {
        for (int i = 0; i < tracks.length; i++) {
            if (tracks[i] != null) tracks[i].dispose();
            tracks[i] = null;
        }
        if (menuTrack != null) menuTrack.dispose();
        menuTrack = null;
        if (repair != null) repair.dispose();
        if (shield != null) shield.dispose();
        if (hit != null) hit.dispose();
        if (gameOver != null) gameOver.dispose();
        if (navigate != null) navigate.dispose();
        if (confirm != null) confirm.dispose();
        repair = shield = hit = gameOver = null;
        navigate = confirm = null;
        currentPhase = 0;
        playing = false;
        menuPlaying = false;
    }
}
