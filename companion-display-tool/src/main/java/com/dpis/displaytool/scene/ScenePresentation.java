package com.dpis.displaytool.scene;

import android.app.Dialog;
import android.view.View;
import android.widget.TextView;

public final class ScenePresentation {
    public enum Kind {
        VIEW,
        DIALOG
    }

    private final Kind kind;
    private final View view;
    private final Dialog dialog;
    private final TextView textView;
    private final float baseSp;
    private final String viewName;
    private final String event;

    private ScenePresentation(
            Kind kind,
            View view,
            Dialog dialog,
            TextView textView,
            float baseSp,
            String viewName,
            String event
    ) {
        this.kind = kind;
        this.view = view;
        this.dialog = dialog;
        this.textView = textView;
        this.baseSp = baseSp;
        this.viewName = viewName;
        this.event = event;
    }

    public static ScenePresentation view(
            View view,
            TextView textView,
            float baseSp,
            String viewName,
            String event
    ) {
        return new ScenePresentation(Kind.VIEW, view, null, textView, baseSp, viewName, event);
    }

    public static ScenePresentation dialog(
            Dialog dialog,
            View root,
            TextView textView,
            float baseSp,
            String viewName,
            String event
    ) {
        return new ScenePresentation(Kind.DIALOG, root, dialog, textView, baseSp, viewName, event);
    }

    public Kind kind() {
        return kind;
    }

    public View view() {
        return view;
    }

    public Dialog dialog() {
        return dialog;
    }

    public TextView textView() {
        return textView;
    }

    public float baseSp() {
        return baseSp;
    }

    public String viewName() {
        return viewName;
    }

    public String event() {
        return event;
    }
}
