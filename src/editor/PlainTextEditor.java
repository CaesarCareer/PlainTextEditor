package editor;

import static editor.Utils.*;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;


public class PlainTextEditor extends Application {
    private List<int[]> findAllList;
    private String tryToFind = "";
    private int findIndex = -1;
    private static Deque<String> recentFile;
    private static Dialog<String> findDialog;
    private static Dialog<FontAttr> fontAttrDialog;
    private static String font_family = DEFAULT_FONT_FAMILY, font_size = DEFAULT_FONT_SIZE;
    private static EventHandler<ActionEvent> menuItemEventHandler;
    private static EventHandler<ActionEvent> checkMenuItemEventHandler;
    private static EventHandler<ActionEvent> fileMenuItemEventHandler;
    private static File choosenFile;
    private static FileChooser fileChooser;
    private static Stage stage;
    private static Scene scene;
    private static VBox vbox;
    private static InlineCssTextArea area;
    private static MenuBar menuBar;
    private static Menu file, edit, format, view;
    private static Menu font, textAlignment, openRecent;
    private static MenuItem newFile, open, save, saveAs, close, undo, redo, cut, copy, paste,
            selectAll, showFonts, minimum, setFullScreen, find;
    private static CheckMenuItem bold, italic, alignLeft, center, alignRight, wrapToPage;
    private static boolean isBold = false, isItalic = false, isLeft = true, isRight = false;


    @Override
    public void start(Stage primaryStage) {
        // initialize area with some lines of text
        stage = primaryStage;
        initEditor();
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Build a dialog which pops up when click on "Find" menuItem
    private Dialog findDialog() {
        Dialog dialog = new Dialog<>();
        dialog.setOnCloseRequest(new EventHandler<DialogEvent>() {
            @Override
            public void handle(DialogEvent e) {
                unmarkAll();
            }
        });
        dialog.setTitle("Find ...");
        dialog.setHeaderText("Please specify…");
        DialogPane dialogPane = dialog.getDialogPane();

        ButtonType findAllButtonType = new ButtonType(BUTTON_FINDALL);
        dialogPane.getButtonTypes().addAll(findAllButtonType, ButtonType.PREVIOUS, ButtonType.NEXT, ButtonType.CLOSE);
        TextField tf = new TextField();
        dialogPane.setContent(new VBox(new Label("Find: "), tf));

        final Button btfa = (Button) dialog.getDialogPane().lookupButton(findAllButtonType);
        final Button btp = (Button) dialog.getDialogPane().lookupButton(ButtonType.PREVIOUS);
        final Button btn = (Button) dialog.getDialogPane().lookupButton(ButtonType.NEXT);
        final Button btc = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);

        btfa.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    if ((findAllList.isEmpty() || !tf.getText().equalsIgnoreCase(tryToFind)) && !tf.getText().isEmpty()) {
                        unmarkAll();
                        tryToFind = tf.getText();
                        findAllList = findAll(tryToFind);
                        findIndex = 0;
                    }

                    for (int[] arr : findAllList) {
                        markStr(arr);
                    }

                    event.consume();

                }
        );
        btp.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    if ((findAllList.isEmpty() || !tf.getText().equalsIgnoreCase((tryToFind))) && !tf.getText().isEmpty()) {
                        unmarkAll();
                        tryToFind = tf.getText();
                        findAllList = findAll(tryToFind);
                        findIndex = 0;
                    }
                    if (!findAllList.isEmpty()) {
                        if (findIndex == 0) {
                            findIndex = findAllList.size() - 1;
                        } else {
                            findIndex--;
                        }
                    }
                    unmarkAll();
                    markStr(findAllList.get(findIndex));

                    event.consume();
                }
        );
        btn.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    if ((findAllList.isEmpty() || !tf.getText().equalsIgnoreCase((tryToFind))) && !tf.getText().isEmpty()) {
                        unmarkAll();
                        tryToFind = tf.getText();
                        findAllList = findAll(tryToFind);
                        findIndex = -1;
                    }
                    if (!findAllList.isEmpty()) {
                        if (findIndex == findAllList.size() - 1) {
                            findIndex = 0;
                        } else {
                            findIndex++;
                        }
                    }
                    unmarkAll();
                    markStr(findAllList.get(findIndex));
                    event.consume();
                }
        );
        btc.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    unmarkAll();
                    findIndex = -1;
                    findAllList = new ArrayList<>();
                    tryToFind = "";
                }
        );
        return dialog;
    }

    // popup the find dialog
    private void findFromFile() {
        findDialog.showAndWait();
    }

    // make the whole file white background
    private void unmarkAll() {
        for (int[] arr : findAllList) {
            unmarkStr(arr);
        }
    }

    // initiate the editor
    private void initEditor() {
        fontAttrDialog = fontAttributeDialog();
        findAllList = new ArrayList<>();
        findDialog = findDialog();
        recentFile = new ArrayDeque<>();
        choosenFile = null;
        menuBar = new MenuBar();
        area = new InlineCssTextArea("");
        area.setMinSize(INIT_VBOX_WIDTH, INIT_VBOX_HEIGHT);
        area.requestFocus();
        stage.setTitle("Untitled");
        vbox = new VBox(menuBar, area);
        menuItemEventHandler = menuItemAction();
        checkMenuItemEventHandler = checkMenuItemAction();
        fileMenuItemEventHandler = fileMenuItemEventHandler();
        scene = new Scene(vbox, INIT_VBOX_WIDTH, INIT_VBOX_HEIGHT);
        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
        initMenuBar();
        initAlignment();
        closePrompt();
    }

    // Update the recent file menu so that user can choose to open the recently edited file
    private void updateRecent(String path) {
        for (String p : recentFile) {
            if (p.equals(path)) {
                recentFile.remove(p);
                break;
            }
        }
        recentFile.addFirst(path);
        if (recentFile.size() > MAX_RECENT) {
            recentFile.removeLast();
        }
        updateRecentMenu();
    }

    // get result from user given peompt
    private boolean confirmationDialog(String prompt) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText(prompt);
        Optional<ButtonType> result = alert.showAndWait();
        return result.get() == ButtonType.OK;
    }

    // initiate the CSS textarea
    private void initArea() {
        area.replaceText("");
        area.setWrapText(false);
        font_family = DEFAULT_FONT_FAMILY;
        font_size = DEFAULT_FONT_SIZE;
        initAlignment();
    }

    // default alignment is left
    private void initAlignment() {
        isLeft = true;
        isRight = false;
        alignLeft.setSelected(true);
        alignRight.setSelected(false);
        center.setSelected(false);
        setAlignment();
    }

    // set alignment to left
    private void setLeft() {
        isLeft = true;
        isRight = false;
        alignLeft.setSelected(true);
        alignRight.setSelected(false);
        center.setSelected(false);
        setAlignment();
    }

    // set alignment to right
    private void setRight() {
        isLeft = false;
        isRight = true;
        alignLeft.setSelected(false);
        alignRight.setSelected(true);
        center.setSelected(false);
        setAlignment();
    }

    // set alignment to center
    private void setCenter() {
        isLeft = false;
        isRight = false;
        alignLeft.setSelected(false);
        alignRight.setSelected(false);
        center.setSelected(true);
        setAlignment();
    }

    // set on checkMenuItems on event handler
    private static void checkMenuItemSetOnAction(EventHandler<ActionEvent> eventHandler) {
        wrapToPage.setOnAction(checkMenuItemEventHandler);
        alignLeft.setOnAction(checkMenuItemEventHandler);
        center.setOnAction(checkMenuItemEventHandler);
        alignRight.setOnAction(checkMenuItemEventHandler);
        bold.setOnAction(checkMenuItemEventHandler);
        italic.setOnAction(checkMenuItemEventHandler);
    }

    // set menuItems on event handler
    private static void menuItemSetOnAction(EventHandler<ActionEvent> eventHandler) {
        newFile.setOnAction(eventHandler);
        open.setOnAction(eventHandler);
        save.setOnAction(eventHandler);
        saveAs.setOnAction(eventHandler);
        close.setOnAction(eventHandler);
        undo.setOnAction(eventHandler);
        redo.setOnAction(eventHandler);
        cut.setOnAction(eventHandler);
        copy.setOnAction(eventHandler);
        paste.setOnAction(eventHandler);
        selectAll.setOnAction(eventHandler);
        showFonts.setOnAction(eventHandler);
        minimum.setOnAction(eventHandler);
        setFullScreen.setOnAction(eventHandler);
        find.setOnAction(eventHandler);
    }

    // Build event handler for menuitems such that menuitems have their respective actions when clicked on
    private EventHandler<ActionEvent> menuItemAction() {
        return new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                MenuItem mItem = (MenuItem) event.getSource();
                String item_name = mItem.getText();
                try {
                    switch (item_name) {
                        case MENUITEM_NEW:
                            create();
                            break;
                        case MENUITEM_OPEN:
                            open();
                            break;
                        case MENUITEM_SAVE:
                            save();
                            break;
                        case MENUITEM_SAVE_AS:
                            saveAs();
                            break;
                        case MENUITEM_CLOSE:
                            close();
                            break;
                        case MENUITEM_UNDO:
                            undo();
                            break;
                        case MENUITEM_REDO:
                            redo();
                            break;
                        case MENUITEM_CUT:
                            cut();
                            break;
                        case MENUITEM_COPY:
                            copy();
                            break;
                        case MENUITEM_PASTE:
                            paste();
                            break;
                        case MENUITEM_SELECT_ALL:
                            selectAll();
                            break;
                        case MENUITEM_SHOW_FONTS:
                            setShowFonts();
                            break;
                        case MENUITEM_MINIMUM:
                            minimum();
                            break;
                        case MENUITEM_ENTER_FULL_SCREEN:
                            setFullScreen();
                            break;
                        case MENUITEM_FIND:
                            findFromFile();
                            break;
                        default:
                            throw new IllegalArgumentException("No such menuitem");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
    }

    // Build event handler for checkmenuitems such that checkmenuitems have their respective actions when clicked on
    private EventHandler<ActionEvent> checkMenuItemAction() {
        return new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                String source = ((CheckMenuItem) e.getSource()).getText();
                switch (source) {
                    case MENUITEM_FONT_BOLD:
                        setBold();
                        break;
                    case MENUITEM_FONT_ITALIC:
                        setItalic();
                        break;
                    case MENUITEM_WRAP_TO_PAGE:
                        wrapToPage();
                        break;
                    case CHECKMENUITEM_ALIGN_LEFT:
                        setLeft();
                        break;
                    case CHECKMENUITEM_ALIGN_RIGHT:
                        setRight();
                        break;
                    case CHECKMENUITEM_ALIGN_CENTER:
                        setCenter();
                        break;
                    default:
                }
            }
        };
    }

    // prompt the user to save the file when the user wants to close the window
    private void closePrompt() {
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                close();
            }
        });
    }

    // decide whether the file page will be saved
    // that is the file should be saved as well as the user wants to save it
    private void saveDecision() {
        if (needToSave() && confirmationDialog(PROMPT_SAVE_FILE)) {
            save();
        }
    }

    // decide whether this file pages needs to be saved
    private boolean needToSave() {
        return (choosenFile == null && !area.getText().isEmpty()) || (choosenFile != null);
    }

    // create a new file
    private void create() {
        saveDecision();

        choosenFile = null;
        initArea();
    }

    // open a file using the absolute path
    private void open(String path) throws IOException {
        saveDecision();
        choosenFile = new File(path);

        if (choosenFile != null) {
            area.replaceText(Files.readString(choosenFile.toPath(), StandardCharsets.ISO_8859_1));
            updateRecent(choosenFile.getAbsolutePath());
        }

    }

    // open a file when user click on the open menuitem
    private void open() throws IOException {
        saveDecision();
        fileChooser.setTitle("Open File");
        choosenFile = fileChooser.showOpenDialog(stage);
        if (choosenFile != null) {
            stage.setTitle(choosenFile.getName());
            area.replaceText(Files.readString(choosenFile.toPath(), StandardCharsets.ISO_8859_1));
            updateRecent(choosenFile.getAbsolutePath());
        }
    }

    // save the file when the file is already choosen, or turn to save as if it is a new file or user wants to create a new file
    private void save() {
        if (choosenFile == null) {
            saveAs();
            return;
        }
        try {
            stage.setTitle(choosenFile.getName());
            PrintWriter writer;
            writer = new PrintWriter(choosenFile);
            writer.println(area.getText());
            writer.close();
            updateRecent(choosenFile.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // save as action
    private void saveAs() {
        choosenFile = fileChooser.showSaveDialog(stage);

        if (choosenFile != null) {
            save();
        }
    }

    // save the file when necessary and then close the window
    private void close() {
        saveDecision();
        stage.close();
    }

    private void undo() {
        area.undo();
    }

    private void redo() {
        area.redo();
    }

    private void cut() {
        area.cut();
    }

    private void copy() {
        area.copy();
    }

    private void paste() {
        area.paste();
    }

    private void selectAll() {
        area.selectAll();
    }

    private void minimum() {
        stage.setIconified(true);
    }

    private void setFullScreen() {
        stage.setFullScreen(true);
    }

    private void wrapToPage() {
        if (area.isWrapText()) {
            area.setWrapText(false);
        } else {
            area.setWrapText(true);
        }
    }

    // update the recent file menu so that users can open these files
    private static void updateRecentMenu() {
        int index = file.getItems().indexOf(openRecent);
        if (index != -1) {
            openRecent = new Menu(MENU_OPEN_RECENT);
            file.getItems().set(index, openRecent);
        }
        List<String> filePaths = new ArrayList<>(recentFile);
        for (String path : filePaths) {
            MenuItem mi = new MenuItem(path);
            mi.setOnAction(fileMenuItemEventHandler);
            openRecent.getItems().add(mi);
        }
    }

    // when user wants to open recently edited files, those menuitems will trigger this event handler
    private EventHandler<ActionEvent> fileMenuItemEventHandler() {
        return new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                String source = ((MenuItem) e.getSource()).getText();
                try {
                    open(source);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    private static void fileMenu() {
        file = new Menu(MENU_FILE);

        newFile = new MenuItem(MENUITEM_NEW);
        open = new MenuItem(MENUITEM_OPEN);
        openRecent = new Menu(MENU_OPEN_RECENT);
        updateRecentMenu();
        save = new MenuItem(MENUITEM_SAVE);
        saveAs = new MenuItem(MENUITEM_SAVE_AS);
        close = new MenuItem(MENUITEM_CLOSE);

        file.getItems().add(newFile);
        file.getItems().add(open);
        file.getItems().add(openRecent);
        file.getItems().add(save);
        file.getItems().add(saveAs);
        file.getItems().add(new SeparatorMenuItem());
        file.getItems().add(close);

        menuBar.getMenus().add(file);
    }

    private static void editMenu() {
        edit = new Menu(MENU_EDIT);

        undo = new MenuItem(MENUITEM_UNDO);
        redo = new MenuItem(MENUITEM_REDO);
        cut = new MenuItem(MENUITEM_CUT);
        copy = new MenuItem(MENUITEM_COPY);
        paste = new MenuItem(MENUITEM_PASTE);
        selectAll = new MenuItem(MENUITEM_SELECT_ALL);
        find = new MenuItem(MENUITEM_FIND);

        edit.getItems().add(undo);
        edit.getItems().add(redo);
        edit.getItems().add(new SeparatorMenuItem());
        edit.getItems().add(cut);
        edit.getItems().add(copy);
        edit.getItems().add(paste);
        edit.getItems().add(selectAll);
        edit.getItems().add(find);
        menuBar.getMenus().add(edit);
    }

    private static void formatMenu() {
        format = new Menu(MENU_FORMAT);

        font = new Menu(MENU_FONT);
        textAlignment = new Menu(MENU_TEXT_ALIGNMENT);
        showFonts = new MenuItem(MENUITEM_SHOW_FONTS);
        bold = new CheckMenuItem(MENUITEM_FONT_BOLD);
        italic = new CheckMenuItem(MENUITEM_FONT_ITALIC);

        alignLeft = new CheckMenuItem(CHECKMENUITEM_ALIGN_LEFT);
        alignLeft.setSelected(true);
        center = new CheckMenuItem(CHECKMENUITEM_ALIGN_CENTER);
        alignRight = new CheckMenuItem(CHECKMENUITEM_ALIGN_RIGHT);

        wrapToPage = new CheckMenuItem(MENUITEM_WRAP_TO_PAGE);

        font.getItems().add(showFonts);
        font.getItems().add(new SeparatorMenuItem());
        font.getItems().add(bold);
        font.getItems().add(italic);

        textAlignment.getItems().add(alignLeft);
        textAlignment.getItems().add(center);
        textAlignment.getItems().add(alignRight);

        format.getItems().add(font);
        format.getItems().add(textAlignment);
        format.getItems().add(new SeparatorMenuItem());
        format.getItems().add(wrapToPage);

        menuBar.getMenus().add(format);
    }

    private static void viewMenu() {
        view = new Menu(MENU_VIEW);
        minimum = new MenuItem(MENUITEM_MINIMUM);
        setFullScreen = new MenuItem(MENUITEM_ENTER_FULL_SCREEN);

        view.getItems().add(minimum);
        view.getItems().add(setFullScreen);

        menuBar.getMenus().add(view);
    }

    // initiate the menubar
    // all menus will be created and all menuitems and checkmenuitems will be set on respective event handlers
    private static void initMenuBar() {
        fileMenu();
        editMenu();
        formatMenu();
        viewMenu();

        menuItemSetOnAction(menuItemEventHandler);
        checkMenuItemSetOnAction(checkMenuItemEventHandler);
    }

    // set style for the whole text file
    private void setStyle() {
        StringBuilder style = new StringBuilder();
        style.append("-fx-font-family: '").append(font_family).append("' ;");
        style.append("-fx-font-size: ").append(font_size).append("px;");
        if (isItalic) {
            style.append(" -fx-font-style: italic ;");
        }
        if (isBold) {
            style.append(" -fx-font-weight: bold;");
        }

        area.setStyle(style.toString());
    }

    private void setBold() {
        isBold = !isBold;
        setStyle();
    }

    private void setItalic() {
        isItalic = !isItalic;
        setStyle();
    }

    // open the show fonts menuitem and set font family and font size for the file
    private void setShowFonts() {
        Optional<FontAttr> optionalResult = fontAttrDialog.showAndWait();
        optionalResult.ifPresent((FontAttr result) -> {
            font_family = result.font_family.equals(FONT_FAMILY) ? font_family : result.font_family;
            font_size = result.font_size.equals(FONT_SIZE) ? font_size : result.font_size;
            setStyle();
        });
    }

    private void setAlignment() {
        String alignment = null;
        if (isLeft) {
            alignment = "-fx-text-alignment: left;";
        } else if (isRight) {
            alignment = "-fx-text-alignment: right;";
        } else {
            alignment = "-fx-text-alignment: center;";
        }
        area.setParagraphStyle(area.getCurrentParagraph(), alignment);
    }

    // Build a dialog which will pop up when the show font item is triggered
    // user can choose font family and font size from the dialog
    public Dialog<FontAttr> fontAttributeDialog() {
        Dialog<FontAttr> fontAttrDialog = new Dialog<>();
        fontAttrDialog.setTitle("Choose Fonts");
        fontAttrDialog.setHeaderText("Please specify…");
        DialogPane dialogPane = fontAttrDialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(new ButtonType("Find All"), ButtonType.OK, ButtonType.CANCEL);
        List<String> ffl = Font.getFamilies();
        ffl.set(0, FONT_FAMILY);
        ObservableList<String> font_family_list =
                FXCollections.observableArrayList(ffl);
        List<String> list = new ArrayList<>();
        list.add(FONT_SIZE);
        for (int i = 1; i <= 100; i++) {
            list.add(i + "");
        }
        ObservableList<String> font_size_list = FXCollections.observableList(list);
        ComboBox<String> ffb = new ComboBox<>(font_family_list);
        ComboBox<String> fsb = new ComboBox<>(font_size_list);
        ffb.getSelectionModel().selectFirst();
        fsb.getSelectionModel().selectFirst();
        dialogPane.setContent(new VBox(8, ffb, fsb));

        fontAttrDialog.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return new FontAttr(ffb.getValue(), fsb.getValue());
            }
            return null;
        });
        return fontAttrDialog;
    }

    // a class used to store result from the choosing font dialog
    private static class FontAttr {
        String font_family;
        String font_size;

        FontAttr(String font_family, String font_size) {
            this.font_family = font_family;
            this.font_size = font_size;
        }
    }

    // get a list of the index range of the string from the file
    private List<int[]> findAll(String s) {
        List<int[]> res = new ArrayList<>();
        String text = area.getText().toLowerCase();
        s = s.toLowerCase();
        int len = s.length();
        int start = 0;
        while (text.indexOf(s, start) != -1) {
            int from = text.indexOf(s, start);
            start = from + len;
            res.add(new int[]{from, start});
        }
        return res;
    }

    // mark the string of respective index range yellow
    private void markStr(int[] arr) {
        area.setStyle(arr[0], arr[1], "-rtfx-background-color: yellow; ");
    }

    // mark the string of index range white, which means unmark the marked string
    private void unmarkStr(int[] arr) {
        area.setStyle(arr[0], arr[1], "-rtfx-background-color: white; ");
    }

    public static void main(String[] args) {
        launch(args);
    }
}






