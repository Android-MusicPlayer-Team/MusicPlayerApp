package com.redmusic.player.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.redmusic.player.R;
import com.redmusic.player.api.MusicApi;
import com.redmusic.player.model.Track;
import com.redmusic.player.player.PlayerHolder;
import com.redmusic.player.util.Prefs;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<String> suggestWords = new ArrayList<>();
    private EditText input;
    private TrackAdapter adapter;
    private LinearLayout historyWrap;
    private TextView emptyTip;
    private RecyclerView suggestList;
    private SuggestAdapter suggestAdapter;
    private int suggestSeq = 0;
    private final Runnable suggestTask = this::fetchSuggest;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        input = view.findViewById(R.id.search_input);
        ImageButton go = view.findViewById(R.id.search_go);
        historyWrap = view.findViewById(R.id.history_wrap);
        emptyTip = view.findViewById(R.id.search_empty);

        adapter = new TrackAdapter(false, false);
        adapter.setAction(new TrackAdapter.OnAction() {
            @Override
            public void onPlay(int position) {
                List<Track> list = adapter.getTracks();
                if (list.isEmpty()) return;
                PlayerHolder.get().playQueue(list, position);
                Prefs.addRecent(list.get(position));
                startActivity(new android.content.Intent(getContext(), PlayerActivity.class));
            }

            @Override
            public void onFav(Track t) {
                Prefs.toggleFav(t);
                Toast.makeText(getContext(), Prefs.isFav(t) ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
            }
        });
        RecyclerView list = view.findViewById(R.id.search_list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);

        // 联想词列表
        suggestList = view.findViewById(R.id.suggest_list);
        suggestList.setLayoutManager(new LinearLayoutManager(getContext()));
        suggestAdapter = new SuggestAdapter(word -> {
            input.setText(word);
            input.setSelection(word.length());
            doSearch();
        });
        suggestList.setAdapter(suggestAdapter);

        // 输入变化：防抖 250ms 后请求联想
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                main.removeCallbacks(suggestTask);
                String q = s.toString().trim();
                if (q.isEmpty()) {
                    hideSuggest();
                    return;
                }
                main.postDelayed(suggestTask, 250);
            }
        });

        go.setOnClickListener(v -> doSearch());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });

        renderHistory();
    }

    private void fetchSuggest() {
        String q = input.getText().toString().trim();
        if (q.isEmpty()) {
            hideSuggest();
            return;
        }
        int seq = ++suggestSeq;
        MusicApi.suggest(q, words -> main.post(() -> {
            if (seq != suggestSeq) return; // 旧请求丢弃
            if (words.isEmpty()) {
                hideSuggest();
                return;
            }
            suggestWords.clear();
            suggestWords.addAll(words);
            suggestAdapter.notifyDataSetChanged();
            suggestList.setVisibility(View.VISIBLE);
        }));
    }

    private void hideSuggest() {
        suggestList.setVisibility(View.GONE);
        suggestWords.clear();
        suggestAdapter.notifyDataSetChanged();
    }

    private void doSearch() {
        String q = input.getText().toString().trim();
        if (q.isEmpty()) {
            Toast.makeText(getContext(), "请输入关键词", Toast.LENGTH_SHORT).show();
            return;
        }
        main.removeCallbacks(suggestTask);
        hideSuggest();
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        Prefs.addHistory(q);
        renderHistory();
        emptyTip.setText("搜索中...");
        emptyTip.setVisibility(View.VISIBLE);
        MusicApi.search(q, new MusicApi.TrackListCallback() {
            @Override
            public void onSuccess(List<Track> tracks) {
                main.post(() -> {
                    adapter.setTracks(tracks);
                    if (tracks.isEmpty()) {
                        emptyTip.setText("没有找到相关歌曲");
                        emptyTip.setVisibility(View.VISIBLE);
                    } else {
                        emptyTip.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String msg) {
                main.post(() -> {
                    emptyTip.setText("网络错误：" + msg);
                    emptyTip.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void renderHistory() {
        historyWrap.removeAllViews();
        List<String> history = Prefs.getHistory();
        if (history.isEmpty()) {
            historyWrap.setVisibility(View.GONE);
            return;
        }
        historyWrap.setVisibility(View.VISIBLE);
        for (String q : history) {
            TextView chip = new TextView(getContext());
            chip.setText(q);
            chip.setTextSize(13);
            chip.setTextColor(0xFFDDDDDD);
            chip.setBackgroundResource(R.drawable.bg_chip);
            int pad = dp(12);
            chip.setPadding(pad, dp(6), pad, dp(6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), dp(8));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                input.setText(q);
                input.setSelection(q.length());
                doSearch();
            });
            historyWrap.addView(chip);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** 联想词列表适配器 */
    private class SuggestAdapter extends RecyclerView.Adapter<SuggestAdapter.VH> {
        interface OnPick { void onPick(String word); }
        private final OnPick pick;

        SuggestAdapter(OnPick pick) { this.pick = pick; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_suggest, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            String w = suggestWords.get(position);
            h.text.setText(w);
            h.itemView.setOnClickListener(v -> pick.onPick(w));
        }

        @Override public int getItemCount() { return suggestWords.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView text;
            VH(@NonNull View itemView) {
                super(itemView);
                text = itemView.findViewById(R.id.suggest_text);
            }
        }
    }
}
