package com.energer.freestylegame.controller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.energer.freestylegame.R;
import com.energer.freestylegame.model.Music;
import com.energer.freestylegame.model.Player;
import com.energer.freestylegame.model.Word;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import pl.droidsonroids.gif.GifImageView;

public class GameActivity extends AppCompatActivity {
    private ArrayList<Player> players;
    private ArrayList<Player> classement;
    private Word word;
    private BottomNavigationView bottomNavInGame;
    private TextView word_1;
    private TextView word_2;
    private TextView word_3;
    private TextView word_4;
    private TextView word_5;
    private TextView current_player;
    private ImageButton btn_win;
    private ImageButton btn_loose;
    private int id_current_player;
    private Button launch;
    private TextView countdown;
    private boolean timer_cont;
    private ProgressBar progress;
    private GifImageView gif;
    private ImageView next_player;
    private View layout_next;
    private TextView next_player_textview;
    private ImageView current_img;
    private TextView current_text;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final CollectionReference wordsDb = db.collection("words");
    private String wordIdStr;
    private int sizeDb;

    private Music music;
    private MediaPlayer music_player;
    private boolean cont;
    private int bridge;
    private int end_verse;
    private int start_verse;
    private boolean vote_ok;
    private final CollectionReference musicDb = db.collection("songs");
    private int sizeMusicDb;
    private String musicIdStr;

    private final CollectionReference plays = db.collection("plays");
    private final CollectionReference playersDb = db.collection("players");

    private boolean a=true;

    private Thread timeThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        music = new Music();
        classement = new ArrayList<Player>();

        //getIntentInfos

        players = Objects.requireNonNull(this.getIntent().getExtras()).getParcelableArrayList("players");

        //deserialization :
        bottomNavInGame = (BottomNavigationView) findViewById(R.id.bottom_navigation_ingame);
        progress = (ProgressBar) findViewById(R.id.progress_bar);
        word_1 = (TextView) findViewById(R.id.word_1);
        word_2 = (TextView) findViewById(R.id.word_2);
        word_3 = (TextView) findViewById(R.id.word_3);
        word_4 = (TextView) findViewById(R.id.word_4);
        word_5 = (TextView) findViewById(R.id.word_5);
        current_player = (TextView) findViewById(R.id.current_player);
        btn_win = (ImageButton) findViewById(R.id.win);
        btn_win.setVisibility(View.INVISIBLE);
        btn_loose = (ImageButton) findViewById(R.id.loose);
        btn_loose.setVisibility(View.INVISIBLE);
        launch = (Button) findViewById(R.id.launch_game);
        launch.setVisibility(View.INVISIBLE);
        music_player = new MediaPlayer();
        countdown = (TextView) findViewById(R.id.countdown);
        gif=(GifImageView)findViewById(R.id.gif);
        next_player=(ImageView)findViewById(R.id.next_player);
        layout_next=findViewById(R.id.layout_next);
        next_player_textview=(TextView)findViewById(R.id.next_player_textview);
        current_img=(ImageView)findViewById(R.id.current_img);
        current_text=(TextView)findViewById(R.id.current_text);


        //listeners :
        bottomNavInGame.setOnNavigationItemSelectedListener(navListener);
        btn_win.setOnClickListener(game_listener);
        btn_loose.setOnClickListener(game_listener);
        launch.setOnClickListener(btn_music_listener);

        //start the game :
        getWordsFromDb();
        getMusicFromDb();

        current_text.setText(R.string.launch_game);
        id_current_player = 0;

        next_player_textview.setText(players.get(id_current_player).getName());
    }

    protected void onStop() {
        cont = false;
        timer_cont = false;
        a=false;
        if (music_player!=null) {
            music_player.stop();
            music_player.release();
            music_player = null;
        }
        if(listenBeat.isAlive()){
            listenBeat.interrupt();
        }
        if(musicThread.isAlive()){
            musicThread.interrupt();
        }
        super.onStop();
    }



    private View.OnClickListener btn_music_listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            progress.setVisibility(View.VISIBLE);
            start_verse = music.getStart_verse();
            end_verse = music.getEnd_verse();
            bridge = music.getStart_bridge();
            cont = true;
            current_text.setText(" ");

            music_player = new MediaPlayer();
            musicThread.start();
            launch.setVisibility(View.GONE);
        }
    };

    //for delete once the "Ecoute du beat"
    private Thread listenBeat=new Thread(new Runnable() {
        @Override
        public void run() {
            while(a){
                if(music_player.getCurrentPosition() >= start_verse - 10000){
                    a=false;
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    current_text.setText(" ");
                }
            });
        }
    });


    //the main thread of the game, it launch the music, the vote, the countdown timer at the right moment
    private Thread musicThread = new Thread(new Runnable() {
        @Override
        public void run() {
            Uri myUri = Uri.parse(music.getUrl());
            try {
                music_player.setDataSource(getApplicationContext(), myUri);
                music_player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                music_player.prepare();
                music_player.start();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        current_text.setText(R.string.listen_beat);
                    }
                });
                listenBeat.start();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                        gif.setVisibility(View.VISIBLE);
                        layout_next.setVisibility(View.VISIBLE);
                    }
                });

                time_to_play();

                while (cont) {
                    if (music_player.getCurrentPosition() == end_verse) {

                        music_player.seekTo(bridge);
                        vote_ok = false;
                        runOnUiThread(vote);
                        while (!vote_ok) {
                            if (music_player.getCurrentPosition() == start_verse) {
                                music_player.seekTo(bridge);
                            }
                        }
                        if (music_player.getCurrentPosition() >= start_verse - 10500) {
                            boolean cond = true;
                            while (cond) {
                                if (music_player.getCurrentPosition() == start_verse) {
                                    cond = false;
                                }
                            }
                            music_player.seekTo(bridge);
                        }
                        time_to_play();

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    });

    //set the view for the vote
    private Runnable vote = new Runnable() {
        @Override
        public void run() {
            btn_loose.setVisibility(View.VISIBLE);
            btn_win.setVisibility(View.VISIBLE);
            String ch = getString(R.string.vote_on) +" "+ players.get(id_current_player).getName();
            current_text.setText(ch);
            current_img.setVisibility(View.GONE);
            current_player.setText("");
        }
    };

    //lauch the countdown timer at the right time
    private Runnable time_to_play_runnable = new Runnable() {
        @Override
        public void run() {
            timer_cont = true;
            while (timer_cont) {
                if (music_player.getCurrentPosition() >= (start_verse - 10000)) {
                    timer.start();
                    timer_cont = false;
                }
            }
        }
    };

    private void time_to_play() {
        timeThread=new Thread(time_to_play_runnable);
        timeThread.start();
    }

    //countdown timer before the freestyle
    private CountDownTimer timer = new CountDownTimer(10000, 1000) {
        @Override
        public void onTick(long l) {
            countdown.setText(Long.toString(l/1000));
        }

        @Override
        public void onFinish() {
            int next = id_current_player + 1;
            if (next == players.size()) {
                next = 0;
            }
            current_player.setText(players.get(id_current_player).getName());
            current_img.setImageResource(R.drawable.mic_player);
            current_img.setVisibility(View.VISIBLE);

            next_player_textview.setText(players.get(next).getName());
            countdown.setText("");

        }
    };

















    private BottomNavigationView.OnNavigationItemSelectedListener navListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

            switch (menuItem.getItemId()){
                case R.id.nav_stopgame:
                    goMain();
                    break;
            }
            return true;
        }
    };



    private View.OnClickListener game_listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String ch;
            switch (view.getId()) {
                case R.id.win:
                    players.get(id_current_player).increaseScore();
                    current_player.setText(players.get(id_current_player).getName());
                    current_img.setImageResource(R.drawable.validate_orange);
                    current_img.setVisibility(View.VISIBLE);
                    id_current_player += 1;
                    current_text.setText("");
                    break;
                case R.id.loose:
                    classement.add(0, players.get(id_current_player));
                    current_player.setText(players.get(id_current_player).getName());
                    current_img.setImageResource(R.drawable.unvalidate_orange);
                    current_img.setVisibility(View.VISIBLE);
                    players.remove(id_current_player);
                    current_text.setText("");
                    break;
            }

            if (id_current_player == players.size()) {
                id_current_player = 0;
            }

            vote_ok = true;

            btn_loose.setVisibility(View.INVISIBLE);
            btn_win.setVisibility(View.INVISIBLE);

            if (players.size() == 1) {
                cont = false;
                classement.add(0, players.get(id_current_player));

                endGame();

                Intent EndgameActivity = new Intent(GameActivity.this, com.energer.freestylegame.controller.EndgameActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("classement", classement);
                EndgameActivity.putExtras(bundle);

                startActivity(EndgameActivity);
            }

        }
    };


    private void endGame() {
        String ch = players.get(id_current_player).getName() +" "+ getString(R.string.has_win);
        current_text.setText(ch);

        setData();


    }

    //upload the stats on Firebase
    private void setData() {
        Map<String, Object> play = new HashMap<>();
        play.put("rank", classement);
        play.put("words", db.collection("words").document(wordIdStr));
        play.put("music", db.collection("songs").document(musicIdStr));
        play.put("date", Calendar.getInstance().getTime());

        final DocumentReference[] game = new DocumentReference[1];
        plays.add(play).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d("play", "DocumentSnapshot written");
                game[0] = documentReference;
                CollectionReference playerRef;
                String ref;
                for (int i = 0; i < classement.size(); i++) {
                    if (classement.get(i).getId().equals("0")) {
                        a = false;
                    } else {
                        a = true;
                        ref = classement.get(i).getId();
                        playerRef = playersDb.document(ref).collection("plays");
                        Map<String, Object> player_play = new HashMap<>();
                        player_play.put("game", game[0]);

                        playerRef.add(player_play).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("player", "DocumentSnapshot written");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("data", "Error adding document player");
                            }
                        });
                    }

                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w("data", "Error adding document");
            }
        });
    }

    private void goMain(){
        startActivity(new Intent(this,MainActivity.class));
    }


    //BDD :

    private void getMusicFromDb() {
        musicDb.document("size").get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                sizeMusicDb = Objects.requireNonNull(documentSnapshot.getLong("size")).intValue();
                int musicId = new Random().nextInt(sizeMusicDb) + 1;
                musicIdStr = String.valueOf(musicId);
                musicDb.document(musicIdStr).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        music = documentSnapshot.toObject(Music.class);
                        launch.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
    }


    private void getWordsFromDb() {
        wordsDb.document("size").get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                sizeDb = Objects.requireNonNull(documentSnapshot.getLong("size")).intValue();
                final int wordId = new Random().nextInt(GameActivity.this.sizeDb) + 1;
                wordIdStr = String.valueOf(wordId);
                wordsDb.document(wordIdStr).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        word = new Word((List<String>) Objects.requireNonNull(documentSnapshot.get("words")), wordId);
                        setWordAff();
                    }
                });
            }
        });
    }

    private void setWordAff() {
        word_1.setText(word.getWords()[0]);
        word_2.setText(word.getWords()[1]);
        word_3.setText(word.getWords()[2]);
        word_4.setText(word.getWords()[3]);
        word_5.setText(word.getWords()[4]);
    }

}
