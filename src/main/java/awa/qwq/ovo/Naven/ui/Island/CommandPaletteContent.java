package awa.qwq.ovo.Naven.ui.Island;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventKey;
import awa.qwq.ovo.Naven.events.impl.EventMouseClick;
import awa.qwq.ovo.Naven.modules.ModuleManager;
import awa.qwq.ovo.Naven.modules.impl.visual.Island;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;

public class CommandPaletteContent implements IslandContent {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static CommandPaletteContent instance;
    private boolean eventsRegistered = false;
    
    private boolean isOpen = false;
    private StringBuilder inputText = new StringBuilder();
    private List<String> suggestions = new ArrayList<>();
    private int selectedSuggestionIndex = 0;
    private long lastEnterPressTime = 0;
    private static final long DOUBLE_ENTER_THRESHOLD = 300;

    private static final List<CommandDefinition> COMMANDS = new ArrayList<>();

    private static final String[] SORRY_TEXTS = {
        "I'm not quite sure what you mean by that. Could you try again?",
        "Hmm, I don't recognize that command. Want to try something else?",
        "Sorry, I couldn't find that command. Maybe check your spelling?",
        "I'm not familiar with that command. What were you trying to do?",
        "That doesn't seem like a command I know. Let me help you find the right one!",
        "Oops, I can't execute that. Could you rephrase it?",
        "I don't think I understand that command. Want some help?",
        "That's not something I can do right now. Try a different command!",
        "I'm not sure how to handle that. Perhaps try typing 'help'?",
        "Sorry, that command isn't available. Is there something else I can help with?"
    };
    
    static {
        COMMANDS.add(new CommandDefinition("toggle", "toggle <module>", "切换模块状态"));
        COMMANDS.add(new CommandDefinition("bind", "bind for <module>", "绑定模块按键"));
        COMMANDS.add(new CommandDefinition("config save", "config save", "保存配置"));
        COMMANDS.add(new CommandDefinition("config load", "config load", "加载配置"));
        COMMANDS.add(new CommandDefinition("open config", "open config directory", "打开配置目录"));
    }
    
    public CommandPaletteContent() {
        instance = this;
    }
    
    public static CommandPaletteContent getInstance() {
        return instance;
    }
    
    public void registerEvents() {
        if (eventsRegistered) {
            unregisterEvents();
        }
        Naven.getInstance().getEventManager().register(this);
        eventsRegistered = true;
    }
    
    public void unregisterEvents() {
        if (eventsRegistered) {
            Naven.getInstance().getEventManager().unregister(this);
            eventsRegistered = false;
        }
    }
    
    @EventTarget(0)
    public void onKey(EventKey e) {
        if (!e.isState()) return;

        Island islandModule =
            (Island) Naven.getInstance()
                .getModuleManager().getModule(Island.class);
        if (islandModule == null || !islandModule.isEnabled()) {
            return;
        }

        int key = e.getKey();
        if (key == InputUtil.GLFW_KEY_PERIOD) {
            if (mc.currentScreen != null && !(mc.currentScreen instanceof DummyScreen)) {
                return;
            }
            if (mc.options != null && mc.options.chatKey.isPressed()) {
                return;
            }
            
            isOpen = !isOpen;
            if (isOpen) {
                inputText = new StringBuilder();
                updateSuggestions();
                if (mc.mouse != null && mc.mouse.isCursorLocked()) {
                    mc.mouse.unlockCursor();
                }
                if (mc.currentScreen == null) {
                    savedScreen = null;
                    mc.setScreen(new DummyScreen());
                } else {
                    // mc.screen 应该是 DummyScreen，保存它以便之后恢复
                    savedScreen = mc.currentScreen;
                }
            } else {
                suggestions.clear();
                if (isBindingMode) {
                    endBindingMode();
                } else {
                    closePanel();
                }
            }
            e.setCancelled(true);
            return;
        }

        if (isOpen) {
            if (isBindingMode) {
                if (e.getKey() == InputUtil.GLFW_KEY_ESCAPE) {
                    onKeyForBinding(e);
                }
                e.setCancelled(true);
                return;
            }

            e.setCancelled(true);

            if (e.getKey() == InputUtil.GLFW_KEY_ESCAPE) {
                isOpen = false;
                inputText = new StringBuilder();
                suggestions.clear();
                keyboardKeys.clear();
                if (isBindingMode) {
                    endBindingMode();
                } else {
                    closePanel();
                }
                return;
            }

            if (e.getKey() == InputUtil.GLFW_KEY_BACKSPACE) {
                if (inputText.length() > 0) {
                    inputText.deleteCharAt(inputText.length() - 1);
                    updateSuggestions();
                }
                return;
            }

            if (e.getKey() == InputUtil.GLFW_KEY_TAB) {
                if (!suggestions.isEmpty() && selectedSuggestionIndex < suggestions.size()) {
                    String selected = suggestions.get(selectedSuggestionIndex);
                    String command = selected.split(" - ")[0];
                    inputText = new StringBuilder(command);
                    if (!command.contains(" ")) {
                        inputText.append(" ");
                    }
                    updateSuggestions();
                }
                return;
            }

            if (e.getKey() == InputUtil.GLFW_KEY_ENTER || e.getKey() == InputUtil.GLFW_KEY_KP_ENTER) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastEnterPressTime < DOUBLE_ENTER_THRESHOLD) {
                    isOpen = false;
                    inputText = new StringBuilder();
                    suggestions.clear();
                    showHelp = false;
                    if (isBindingMode) {
                        endBindingMode();
                    } else {
                        closePanel();
                    }
                    lastEnterPressTime = 0;
                    return;
                }
                lastEnterPressTime = currentTime;
                executeCommand();
                return;
            }

            if (e.getKey() == InputUtil.GLFW_KEY_UP) {
                if (!suggestions.isEmpty()) {
                    selectedSuggestionIndex = Math.max(0, selectedSuggestionIndex - 1);
                }
                return;
            }
            if (e.getKey() == InputUtil.GLFW_KEY_DOWN) {
                if (!suggestions.isEmpty()) {
                    selectedSuggestionIndex = Math.min(suggestions.size() - 1, selectedSuggestionIndex + 1);
                }
                return;
            }

            if (e.getKey() >= 48 && e.getKey() <= 57) {
                char digit = (char)('0' + (e.getKey() - 48));
                handleCharTyped(digit);
                return;
            }
            if (e.getKey() >= 65 && e.getKey() <= 90) {
                boolean isShift = InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT) || 
                                InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.GLFW_KEY_RIGHT_SHIFT);
                char letter = isShift ? (char)('A' + (e.getKey() - 65)) : (char)('a' + (e.getKey() - 65));
                handleCharTyped(letter);
                return;
            }
            if (e.getKey() == InputUtil.GLFW_KEY_SPACE) {
                handleCharTyped(' ');
            }
        }
    }

    public void handleCharTyped(char codePoint) {
        if (!isOpen || isBindingMode) return;

        if (Character.isISOControl(codePoint)) return;

        if (codePoint == '\t' || codePoint == '\n' || codePoint == '\r') return;
        
        inputText.append(codePoint);
        updateSuggestions();
    }
    
    private void updateSuggestions() {
        String input = inputText.toString().trim().toLowerCase();
        suggestions.clear();
        
        if (input.isEmpty()) {
            return;
        }

        List<String> matches = new ArrayList<>();

        String[] parts = input.split("\\s+");
        if (parts.length == 1) {
            String cmdName = parts[0].toLowerCase();

            if (cmdName.equals("toggle")) {
                ModuleManager moduleManager = Naven.getInstance().getModuleManager();
                for (Module module : moduleManager.getModules()) {
                    matches.add("toggle " + module.getName());
                }
            } else if (cmdName.equals("bind")) {
                ModuleManager moduleManager = Naven.getInstance().getModuleManager();
                for (Module module : moduleManager.getModules()) {
                    matches.add("bind for " + module.getName());
                }
            } else {
                for (CommandDefinition cmd : COMMANDS) {
                    if (cmd.name.toLowerCase().startsWith(input)) {
                        matches.add(cmd.name + " - " + cmd.description);
                    }
                }

                ModuleManager moduleManager = Naven.getInstance().getModuleManager();
                for (Module module : moduleManager.getModules()) {
                    if (module.getName().toLowerCase().startsWith(input)) {
                        matches.add("toggle " + module.getName());
                        matches.add("bind for " + module.getName());
                    }
                }
            }
        } else if (parts.length == 2) {
            String cmdName = parts[0].toLowerCase();
            String arg = parts[1].toLowerCase();
            
            if (cmdName.equals("toggle")) {
                ModuleManager moduleManager = Naven.getInstance().getModuleManager();
                for (Module module : moduleManager.getModules()) {
                    if (module.getName().toLowerCase().startsWith(arg)) {
                        matches.add(cmdName + " " + module.getName());
                    }
                }
            } else if (cmdName.equals("bind")) {
                boolean isForPrefix = false;
                if (arg.equals("for")) {
                    isForPrefix = true;
                } else if (arg.length() > 0 && arg.length() <= "for".length()) {
                    String forPrefix = "for".substring(0, arg.length());
                    if (forPrefix.equals(arg)) {
                        isForPrefix = true;
                    }
                }
                
                if (isForPrefix) {
                    ModuleManager moduleManager = Naven.getInstance().getModuleManager();
                    for (Module module : moduleManager.getModules()) {
                        matches.add("bind for " + module.getName());
                    }
                } else {
                    ModuleManager moduleManager = Naven.getInstance().getModuleManager();
                    for (Module module : moduleManager.getModules()) {
                        if (module.getName().toLowerCase().startsWith(arg)) {
                            matches.add("bind for " + module.getName());
                        }
                    }
                }
            }
        } else if (parts.length == 3) {
            String cmdName = parts[0].toLowerCase();
            String middle = parts[1].toLowerCase();
            String arg = parts[2].toLowerCase();
            
            if (cmdName.equals("bind") && middle.equals("for")) {
                ModuleManager moduleManager = Naven.getInstance().getModuleManager();
                for (Module module : moduleManager.getModules()) {
                    if (module.getName().toLowerCase().startsWith(arg)) {
                        matches.add("bind for " + module.getName());
                    }
                }
            }
        }

        suggestions = matches.stream().limit(8).collect(Collectors.toList());
        selectedSuggestionIndex = Math.min(selectedSuggestionIndex, suggestions.size() - 1);
        if (selectedSuggestionIndex < 0) selectedSuggestionIndex = 0;
    }
    
    private void executeCommand() {
        String command = inputText.toString().trim();
        if (command.isEmpty()) {
            showAllCommands();
            return;
        }
        
        String[] parts = command.split("\\s+");
        if (parts.length == 0) return;
        
        String cmdName = parts[0].toLowerCase();
        String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
        
        try {
            boolean success = false;
            
            switch (cmdName) {
                case "toggle":
                    if (args.length >= 1) {
                        Module module = Naven.getInstance().getModuleManager().getModule(args[0]);
                        if (module != null) {
                            module.toggle();
                            success = true;
                        }
                    }
                    break;
                    
                case "bind":
                    if (args.length >= 2 && args[0].equalsIgnoreCase("for")) {
                        Module module = Naven.getInstance().getModuleManager().getModule(args[1]);
                        if (module != null) {
                            startBindingMode(module);
                            success = true;
                        }
                    }
                    break;
                    
                case "config":
                    if (args.length >= 1) {
                        String action = args[0].toLowerCase();
                        if (action.equals("save")) {
                            Naven.getInstance().getFileManager().save();
                            success = true;
                        } else if (action.equals("load")) {
                            Naven.getInstance().getFileManager().load();
                            success = true;
                        }
                    }
                    break;
                    
                case "open":
                    if (args.length >= 1 && args[0].equalsIgnoreCase("config")) {
                        try {
                            File configDir = new File("config/blinkfix");
                            if (!configDir.exists()) {
                                configDir.mkdirs();
                            }
                            Desktop.getDesktop().open(configDir);
                            success = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
            
            if (!success) {
                showError();
            } else {
                if (!isBindingMode) {
                    isOpen = false;
                    inputText = new StringBuilder();
                    suggestions.clear();
                    showHelp = false;
                    closePanel();
                } else {
                    inputText = new StringBuilder();
                    suggestions.clear();
                    showHelp = false;
                }
            }
        } catch (Exception e) {
            showError();
            e.printStackTrace();
        }
    }
    
    private boolean isBindingMode = false;
    private Module bindingModule = null;
    private net.minecraft.client.gui.screen.Screen savedScreen = null;

    private static class KeyButton {
        float x, y, width, height;
        int keyCode;
        String label;
        boolean isSpecial;
        
        KeyButton(float x, float y, float width, float height, int keyCode, String label, boolean isSpecial) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.keyCode = keyCode;
            this.label = label;
            this.isSpecial = isSpecial;
        }
    }
    
    private List<KeyButton> keyboardKeys = new ArrayList<>();
    
    private void startBindingMode(Module module) {
        isBindingMode = true;
        bindingModule = module;
        inputText = new StringBuilder("bind for " + module.getName());
        buildKeyboardLayout();

        savedScreen = mc.currentScreen;
        if (mc.currentScreen == null) {
            mc.setScreen(new DummyScreen());
        }

        if (mc.mouse != null && mc.mouse.isCursorLocked()) {
            mc.mouse.unlockCursor();
        }
    }
    
    private void endBindingMode() {
        isBindingMode = false;
        bindingModule = null;
        keyboardKeys.clear();
        closePanel();
    }

    private void closePanel() {
        if (savedScreen != null) {
            if (savedScreen instanceof DummyScreen) {
                mc.setScreen(null);
            } else {
                mc.setScreen(savedScreen);
            }
            savedScreen = null;
        } else if (mc.currentScreen instanceof DummyScreen) {
            mc.setScreen(null);
        }

        if (mc.currentScreen == null && mc.mouse != null && !mc.mouse.isCursorLocked()) {
            mc.mouse.lockCursor();
        }
    }

    private static class DummyScreen extends net.minecraft.client.gui.screen.Screen {
        public DummyScreen() {
            super(net.minecraft.text.Text.empty());
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }
        
        @Override
        public void render(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
        }
        
        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }
    
    private void buildKeyboardLayout() {
        keyboardKeys.clear();
        float startX = 0f;
        float startY = 0f;
        float keyWidth = 28f;
        float keyHeight = 28f;
        float spacing = 2f;
        float rowY = startY;

        keyboardKeys.add(new KeyButton(startX, rowY, keyWidth, keyHeight, InputUtil.GLFW_KEY_GRAVE_ACCENT, "~", false));
        float currentX = startX + keyWidth + spacing;

        for (int i = 1; i <= 10; i++) {
            int keyCode = i == 10 ? InputUtil.GLFW_KEY_0 : InputUtil.GLFW_KEY_1 + (i - 1);
            String label = i == 10 ? "0" : String.valueOf(i);
            keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth, keyHeight, keyCode, label, false));
            currentX += keyWidth + spacing;
        }

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth, keyHeight, InputUtil.GLFW_KEY_MINUS, "- =", false));
        currentX += keyWidth + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth, keyHeight, InputUtil.GLFW_KEY_EQUAL, "+ =", false));
        currentX += keyWidth + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth * 2.3f + spacing * 0.5f, keyHeight, InputUtil.GLFW_KEY_BACKSPACE, "Backspace", true));
        
        rowY += keyHeight + spacing;

        keyboardKeys.add(new KeyButton(startX, rowY, keyWidth * 1.5f + spacing * 0.5f, keyHeight, InputUtil.GLFW_KEY_TAB, "Tab", true));
        currentX = startX + keyWidth * 1.5f + spacing * 0.5f + spacing;

        String[] letters1 = {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"};
        int[] keyCodes1 = {InputUtil.GLFW_KEY_Q, InputUtil.GLFW_KEY_W, InputUtil.GLFW_KEY_E, InputUtil.GLFW_KEY_R, 
                          InputUtil.GLFW_KEY_T, InputUtil.GLFW_KEY_Y, InputUtil.GLFW_KEY_U, InputUtil.GLFW_KEY_I, 
                          InputUtil.GLFW_KEY_O, InputUtil.GLFW_KEY_P};
        for (int i = 0; i < 10; i++) {
            keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth, keyHeight, keyCodes1[i], letters1[i], false));
            currentX += keyWidth + spacing;
        }

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth, keyHeight, InputUtil.GLFW_KEY_LEFT_BRACKET, "[ {", false));
        currentX += keyWidth + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth, keyHeight, InputUtil.GLFW_KEY_RIGHT_BRACKET, "] }", false));
        currentX += keyWidth + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth * 1.8f, keyHeight, InputUtil.GLFW_KEY_BACKSLASH, "| \\", false));

        rowY += keyHeight + spacing;

        keyboardKeys.add(new KeyButton(startX, rowY, keyWidth * 1.75f + spacing * 0.75f, keyHeight, InputUtil.GLFW_KEY_CAPS_LOCK, "Caps", true));
        currentX = startX + keyWidth * 1.75f + spacing * 0.75f + spacing;

        String[] letters2 = {"A", "S", "D", "F", "G", "H", "J", "K", "L"};
        int[] keyCodes2 = {InputUtil.GLFW_KEY_A, InputUtil.GLFW_KEY_S, InputUtil.GLFW_KEY_D, InputUtil.GLFW_KEY_F, 
                          InputUtil.GLFW_KEY_G, InputUtil.GLFW_KEY_H, InputUtil.GLFW_KEY_J, InputUtil.GLFW_KEY_K, 
                          InputUtil.GLFW_KEY_L};
        for (int i = 0; i < 9; i++) {
            keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth, keyHeight, keyCodes2[i], letters2[i], false));
            currentX += keyWidth + spacing;
        }

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth, keyHeight, InputUtil.GLFW_KEY_SEMICOLON, "; :", false));
        currentX += keyWidth + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth, keyHeight, InputUtil.GLFW_KEY_APOSTROPHE, "' \"", false));
        currentX += keyWidth + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth * 2.55f + spacing * 0.5f, keyHeight, InputUtil.GLFW_KEY_ENTER, "Enter", true));
        
        rowY += keyHeight + spacing;

        keyboardKeys.add(new KeyButton(startX, rowY, keyWidth * 2.25f + spacing * 1.25f, keyHeight, InputUtil.GLFW_KEY_LEFT_SHIFT, "Shift", true));
        currentX = startX + keyWidth * 2.25f + spacing * 1.25f + spacing;

        String[] letters3 = {"Z", "X", "C", "V", "B", "N", "M"};
        int[] keyCodes3 = {InputUtil.GLFW_KEY_Z, InputUtil.GLFW_KEY_X, InputUtil.GLFW_KEY_C, InputUtil.GLFW_KEY_V, 
                          InputUtil.GLFW_KEY_B, InputUtil.GLFW_KEY_N, InputUtil.GLFW_KEY_M};
        for (int i = 0; i < 7; i++) {
            keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth, keyHeight, keyCodes3[i], letters3[i], false));
            currentX += keyWidth + spacing;
        }

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth, keyHeight, InputUtil.GLFW_KEY_COMMA, ", <", false));
        currentX += keyWidth + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth, keyHeight, InputUtil.GLFW_KEY_PERIOD, ". >", false));
        currentX += keyWidth + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth, keyHeight, InputUtil.GLFW_KEY_SLASH, "/ ?", false));
        currentX += keyWidth + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth * 3.05f + spacing * 1.25f, keyHeight, InputUtil.GLFW_KEY_RIGHT_SHIFT, "Shift", true));
        
        rowY += keyHeight + spacing;

        keyboardKeys.add(new KeyButton(startX, rowY, keyWidth * 1.25f + spacing * 0.25f, keyHeight, InputUtil.GLFW_KEY_LEFT_CONTROL, "Ctrl", true));
        currentX = startX + keyWidth * 1.25f + spacing * 0.25f + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth * 1.25f + spacing * 0.25f, keyHeight, InputUtil.GLFW_KEY_LEFT_SUPER, "Win", true));
        currentX += keyWidth * 1.25f + spacing * 0.25f + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth * 1.25f + spacing * 0.25f, keyHeight, InputUtil.GLFW_KEY_LEFT_ALT, "Alt", true));
        currentX += keyWidth * 1.25f + spacing * 0.25f + spacing;

        float spaceKeyWidth = keyWidth * 6.5f + spacing * 5.5f;
        keyboardKeys.add(new KeyButton(currentX, rowY, spaceKeyWidth, keyHeight, InputUtil.GLFW_KEY_SPACE, "Space", false));
        currentX += spaceKeyWidth + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth * 1.25f + spacing * 0.25f, keyHeight, InputUtil.GLFW_KEY_RIGHT_ALT, "Alt", true));
        currentX += keyWidth * 1.25f + spacing * 0.25f + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth * 1.25f + spacing * 0.25f, keyHeight, InputUtil.GLFW_KEY_RIGHT_SUPER, "Win", true));
        currentX += keyWidth * 1.25f + spacing * 0.25f + spacing;

        int menuKeyCode = 348;
        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth * 1.25f + spacing * 0.25f, keyHeight, menuKeyCode, "Menu", true));
        currentX += keyWidth * 1.25f + spacing * 0.25f + spacing;

        keyboardKeys.add(new KeyButton(currentX, rowY, keyWidth * 1.25f + spacing * 0.25f, keyHeight, InputUtil.GLFW_KEY_RIGHT_CONTROL, "Ctrl", true));
    }
    
    @EventTarget
    public void onKeyForBinding(EventKey e) {
        if (isBindingMode && e.isState() && bindingModule != null) {
            if (e.getKey() == InputUtil.GLFW_KEY_ESCAPE) {
                isOpen = false;
                inputText = new StringBuilder();
                suggestions.clear();
                endBindingMode();
                e.setCancelled(true);
            }
        }
    }
    
    @EventTarget(0)
    public void onMouseClick(EventMouseClick e) {
        if (!isBindingMode || !e.isState() || bindingModule == null) return;

        if (e.getKey() == 0) {
            float mouseX = (float) mc.mouse.getX();
            float mouseY = (float) mc.mouse.getY();

            int screenWidth = mc.getWindow().getWidth();
            int screenHeight = mc.getWindow().getHeight();
            int guiWidth = mc.getWindow().getScaledWidth();
            int guiHeight = mc.getWindow().getScaledHeight();
            float guiMouseX = mouseX * guiWidth / screenWidth;
            float guiMouseY = mouseY * guiHeight / screenHeight;

            IslandManager manager = null;
            try {
                Island islandModule = (Island) Naven.getInstance().getModuleManager().getModule(Island.class);
                if (islandModule != null) {
                    Field field = islandModule.getClass().getDeclaredField("islandManager");
                    field.setAccessible(true);
                    manager = (IslandManager) field.get(islandModule);
                }
            } catch (Exception ignored) {
            }

            float panelX = manager != null ? manager.getPosX() : (guiWidth - IslandManager.getAnimW().value) / 2.0f;
            float panelY = manager != null ? manager.getPosY() : guiHeight * 0.05f;

            float panelContentHeight = 12f * 2 + (float) Fonts.harmony.getHeight(true, 0.4f) + 8f;
            float keyboardX = panelX + 23f;
            float keyboardY = panelY + panelContentHeight - 15f;

            for (KeyButton key : keyboardKeys) {
                float keyX = keyboardX + key.x;
                float keyY = keyboardY + key.y;

                if (guiMouseX >= keyX && guiMouseX <= keyX + key.width && 
                    guiMouseY >= keyY && guiMouseY <= keyY + key.height) {
                    e.setCancelled(true);
                    
                    bindingModule.setKey(key.keyCode);
                    Naven.getInstance().getFileManager().save();

                    String keyName = key.label;
                    String successMessage = "bind for " + bindingModule.getName() + " to " + keyName + ".";
                    ErrorMessageContent errorContent = ErrorMessageContent.getInstance();
                    if (errorContent != null) {
                        errorContent.showSuccess(successMessage);
                    }
                    
                    isOpen = false;
                    inputText = new StringBuilder();
                    suggestions.clear();
                    endBindingMode();
                    return;
                }
            }
        }
    }
    
    private boolean showHelp = false;
    
    private void showError() {
        Random random = new Random();
        String errorMessage = SORRY_TEXTS[random.nextInt(SORRY_TEXTS.length)];

        ErrorMessageContent errorContent = ErrorMessageContent.getInstance();
        if (errorContent != null) {
            errorContent.showError(errorMessage);
        }

        isOpen = false;
        inputText = new StringBuilder();
        suggestions.clear();
        showHelp = false;
        closePanel();
    }
    
    private void showAllCommands() {
        showHelp = true;
        suggestions.clear();
        for (CommandDefinition cmd : COMMANDS) {
            suggestions.add(cmd.usage + " - " + cmd.description);
        }
        if (suggestions.size() > 8) {
            suggestions = suggestions.subList(0, 8);
        }
    }
    
    @Override
    public int getPriority() {
        return 200;
    }
    
    @Override
    public boolean shouldDisplay() {
        return isOpen;
    }

    @Override
    public void render(DrawContext graphics, MatrixStack stack, float x, float y) {
        if (!isOpen) return;

        float padding = 12f;
        float textY = y + padding;
        float scale = 0.4f;

        String displayText;
        if (isBindingMode && bindingModule != null) {
            displayText = "> bind for " + bindingModule.getName();
        } else {
            displayText = "> " + inputText.toString();
        }
        Fonts.harmony.render(stack, displayText, x + padding, textY, Color.WHITE, true, scale);
        
        float inputHeight = (float) Fonts.harmony.getHeight(true, scale);
        float suggestionsY = textY + inputHeight + 8f;

        if (inputText.toString().trim().isEmpty() && !isBindingMode && !showHelp) {
            String hint = "Type a command or press Enter for help...";
            Fonts.harmony.render(stack, hint, x + padding, suggestionsY, new Color(150, 150, 150, 255), true, scale * 0.9f);
        }

        if ((!suggestions.isEmpty() || showHelp) && !isBindingMode) {
            if (showHelp && suggestions.isEmpty()) {
                showAllCommands();
            }
            for (int i = 0; i < suggestions.size(); i++) {
                String suggestion = suggestions.get(i);
                Color color = (i == selectedSuggestionIndex) ? new Color(100, 150, 255, 255) : new Color(200, 200, 200, 255);
                Fonts.harmony.render(stack, "  " + suggestion, x + padding, suggestionsY + i * (inputHeight + 4f), color, true, scale * 0.9f);
            }
        }

        if (isBindingMode && !keyboardKeys.isEmpty()) {
            renderKeyboard(graphics, stack, x, y);
        }
    }
    
    private void renderKeyboard(DrawContext graphics, MatrixStack stack, float panelX, float panelY) {
        float keyboardX = panelX + 23;
        float panelContentHeight = 12f * 2 + (float) Fonts.harmony.getHeight(true, 0.4f) + 8f;
        float keyboardY = panelY + panelContentHeight - 15f;

        float mouseX = (float) mc.mouse.getX();
        float mouseY = (float) mc.mouse.getY();
        int screenWidth = mc.getWindow().getWidth();
        int screenHeight = mc.getWindow().getHeight();
        int guiWidth = mc.getWindow().getScaledWidth();
        int guiHeight = mc.getWindow().getScaledHeight();
        float guiMouseX = mouseX * guiWidth / screenWidth;
        float guiMouseY = mouseY * guiHeight / screenHeight;

        for (KeyButton key : keyboardKeys) {
            float keyX = keyboardX + key.x;
            float keyY = keyboardY + key.y;

            boolean hovered = guiMouseX >= keyX && guiMouseX <= keyX + key.width && guiMouseY >= keyY && guiMouseY <= keyY + key.height;

            Color bgColor;
            if (hovered) {
                bgColor = new Color(102, 204, 255, 200);
            } else {
                bgColor = key.isSpecial ? new Color(25,25,25, 80) : new Color(102, 102, 106, 113);
            }

            if (!hovered) {
                RenderUtils.drawRoundedRect(stack, keyX + 1f, keyY + 1f, key.width, key.height, 4f, new Color(0, 0, 0, 60).getRGB());
            }

            RenderUtils.drawRoundedRect(stack, keyX, keyY, key.width, key.height, 4f, bgColor.getRGB());

            Color borderColor = hovered ? new Color(150, 200, 255, 180) : new Color(100, 100, 130, 100);
            RenderUtils.drawRoundedRect(stack, keyX + 0.5f, keyY + 0.5f, key.width - 1f, key.height - 1f, 3.5f, borderColor.getRGB());

            if (hovered) {
                RenderUtils.drawRoundedRect(stack, keyX - 1f, keyY - 1f, key.width + 2f, key.height + 2f, 5f, new Color(100, 150, 255, 40).getRGB());
            }

            float textScale = key.label.length() > 3 ? 0.25f : 0.3f;
            float textX = keyX + key.width / 2f - Fonts.harmony.getWidth(key.label, textScale) / 2f;
            float keyTextY = (float) (keyY + key.height / 2f - Fonts.harmony.getHeight(true, textScale) / 2f);
            Fonts.harmony.render(stack, key.label, textX, keyTextY, Color.WHITE, true, textScale);
        }
    }
    
    @Override
    public float getWidth() {
        if (!isOpen) return 200;

        if (isBindingMode && !keyboardKeys.isEmpty()) {
            float maxKeyX = 0;
            for (KeyButton key : keyboardKeys) {
                maxKeyX = Math.max(maxKeyX, key.x + key.width);
            }
            return Math.max(getInputWidth(), maxKeyX + 50f);
        }
        
        float padding = 12f * 2;
        float scale = 0.4f;
        String input = inputText.toString();
        
        float maxWidth = Fonts.harmony.getWidth("> " + input, scale);

        for (String suggestion : suggestions) {
            float width = Fonts.harmony.getWidth("  " + suggestion, scale * 0.9f);
            maxWidth = Math.max(maxWidth, width);
        }

        if (input.trim().isEmpty() && !isBindingMode && !showHelp) {
            float hintWidth = Fonts.harmony.getWidth("Type a command or press Enter for help...", scale * 0.9f);
            maxWidth = Math.max(maxWidth, hintWidth);
        }
        
        return maxWidth + padding;
    }
    
    private float getInputWidth() {
        float padding = 12f * 2;
        float scale = 0.4f;
        String input = inputText.toString();
        float maxWidth = Fonts.harmony.getWidth("> " + input, scale);
        
        for (String suggestion : suggestions) {
            float width = Fonts.harmony.getWidth("  " + suggestion, scale * 0.9f);
            maxWidth = Math.max(maxWidth, width);
        }
        
        if (input.trim().isEmpty() && !isBindingMode && !showHelp) {
            float hintWidth = Fonts.harmony.getWidth("Type a command or press Enter for help...", scale * 0.9f);
            maxWidth = Math.max(maxWidth, hintWidth);
        }
        
        return maxWidth + padding;
    }
    
    @Override
    public float getHeight() {
        if (!isOpen) return 40;
        
        float padding = 12f * 2;
        float scale = 0.4f;
        float lineHeight = (float) Fonts.harmony.getHeight(true, scale);
        
        float height = padding + lineHeight;
        
        if (inputText.toString().trim().isEmpty() && !isBindingMode && !showHelp) {
            height += 8f + lineHeight * 0.9f;
        } else if ((!suggestions.isEmpty() || showHelp) && !isBindingMode) {
            height += 8f + suggestions.size() * (lineHeight * 0.9f + 4f);
        }

        if (isBindingMode && !keyboardKeys.isEmpty()) {
            float maxKeyY = 0;
            for (KeyButton key : keyboardKeys) {
                maxKeyY = Math.max(maxKeyY, key.y + key.height);
            }
            height += 10f + maxKeyY;
        }
        
        return height;
    }
    
    private static class CommandDefinition {
        String name;
        String usage;
        String description;
        
        CommandDefinition(String name, String usage, String description) {
            this.name = name;
            this.usage = usage;
            this.description = description;
        }
    }
}
