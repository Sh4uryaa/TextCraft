package com.novaedit.util;

import javafx.scene.Node;
import javafx.scene.control.Label;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central icon factory providing sharp, scalable icons via Ikonli Feather pack
 * with graceful text-glyph fallbacks.
 */
public class Icons {
    private static final Logger log = LoggerFactory.getLogger(Icons.class);

    public static Node get(String name, int iconSize) {
        if (name == null || name.isBlank()) {
            return new Label("");
        }

        try {
            // Map common identifiers to Feather icons
            Feather icon = switch (name.toLowerCase()) {
                case "file-plus", "new", "file_new" -> Feather.FILE_PLUS;
                case "folder", "open", "folder_open" -> Feather.FOLDER;
                case "save" -> Feather.SAVE;
                case "save-all", "save_all" -> Feather.ARCHIVE;
                case "undo" -> Feather.CORNER_UP_LEFT;
                case "redo" -> Feather.CORNER_UP_RIGHT;
                case "cut" -> Feather.SCISSORS;
                case "copy" -> Feather.COPY;
                case "paste" -> Feather.CLIPBOARD;
                case "select-all" -> Feather.CHECK_SQUARE;
                case "search", "find" -> Feather.SEARCH;
                case "replace" -> Feather.REPEAT;
                case "find-files" -> Feather.FILE_TEXT;
                case "goto-line", "jump" -> Feather.CORNER_DOWN_RIGHT;
                case "format", "align" -> Feather.ALIGN_LEFT;
                case "indent" -> Feather.CHEVRONS_RIGHT;
                case "outdent" -> Feather.CHEVRONS_LEFT;
                case "comment" -> Feather.MESSAGE_SQUARE;
                case "duplicate" -> Feather.LAYERS;
                case "move-up" -> Feather.ARROW_UP;
                case "move-down" -> Feather.ARROW_DOWN;
                case "delete", "trash" -> Feather.TRASH_2;
                case "fold" -> Feather.MINIMIZE_2;
                case "unfold" -> Feather.MAXIMIZE_2;
                case "brackets" -> Feather.CODE;
                case "snippets" -> Feather.BOOK_OPEN;
                case "autocomplete" -> Feather.CPU;
                case "wrap" -> Feather.CORNER_DOWN_LEFT;
                case "whitespace" -> Feather.SUN;
                case "numbers" -> Feather.LIST;
                case "minimap" -> Feather.MAP;
                case "split" -> Feather.COLUMNS;
                case "zoom-in" -> Feather.ZOOM_IN;
                case "zoom-out" -> Feather.ZOOM_OUT;
                case "zoom-reset" -> Feather.REFRESH_CW;
                case "fullscreen" -> Feather.MAXIMIZE;
                case "palette", "command" -> Feather.COMMAND;
                case "diff", "compare" -> Feather.GIT_PULL_REQUEST;
                case "record" -> Feather.RADIO;
                case "play" -> Feather.PLAY;
                case "spell" -> Feather.CHECK_CIRCLE;
                case "terminal" -> Feather.TERMINAL;
                case "git" -> Feather.GIT_BRANCH;
                case "commit" -> Feather.GIT_COMMIT;
                case "pull" -> Feather.DOWNLOAD;
                case "push" -> Feather.UPLOAD;
                case "refresh" -> Feather.REFRESH_CCW;
                case "theme" -> Feather.MOON;
                case "settings" -> Feather.SETTINGS;
                case "help" -> Feather.HELP_CIRCLE;
                case "close" -> Feather.X;
                case "explorer" -> Feather.FOLDER;
                case "outline" -> Feather.COMPASS;
                default -> Feather.FILE;
            };

            FontIcon fontIcon = new FontIcon(icon);
            fontIcon.setIconSize(iconSize);
            fontIcon.getStyleClass().add("nova-icon");
            return fontIcon;
        } catch (Exception e) {
            log.debug("Icon fallback for {}: {}", name, e.getMessage());
            Label fallback = new Label("•");
            fallback.setStyle("-fx-font-size: " + iconSize + "px;");
            return fallback;
        }
    }

    public static Node get(String name) {
        return get(name, 14);
    }
}
