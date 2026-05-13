package com.dpis.displaytool.scene;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dpis.displaytool.R;

final class RecyclerTextBindScene implements DisplayScene {
    @Override
    public String id() {
        return "recycler_text_bind";
    }

    @Override
    public boolean supportsVariant(String variant) {
        return TextSceneSupport.supportsNormalFragile(variant);
    }

    @Override
    public ScenePresentation create(SceneRuntime runtime, String variant) {
        View root = runtime.inflater().inflate(R.layout.scene_recycler_text_bind, runtime.detailHost(), false);
        RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(runtime.activity()));
        recyclerView.setAdapter(new TextAdapter(variant));
        return ScenePresentation.view(
                root,
                null,
                TextSceneSupport.BASE_SP,
                TextSceneSupport.VIEW_PRIMARY,
                "recycler_first_screen_stable"
        );
    }

    private static final class TextAdapter extends RecyclerView.Adapter<TextHolder> {
        private final String variant;

        TextAdapter(String variant) {
            this.variant = variant;
        }

        @Override
        public TextHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recycler_text, parent, false);
            return new TextHolder(view);
        }

        @Override
        public void onBindViewHolder(TextHolder holder, int position) {
            holder.textView.setText("Recycler row " + position);
            if (TextSceneSupport.isFragile(variant)) {
                TextSceneSupport.applyDoubleScaledPx(holder.textView, TextSceneSupport.BASE_SP);
            } else {
                TextSceneSupport.applySp(holder.textView, TextSceneSupport.BASE_SP);
            }
        }

        @Override
        public int getItemCount() {
            return 8;
        }
    }

    private static final class TextHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        TextHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text_primary);
        }
    }
}
