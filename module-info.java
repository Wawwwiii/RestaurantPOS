module projectjava {
    requires javafx.controls;
    requires javafx.swing;
    requires javafx.media;
    requires java.desktop;
    requires java.logging;
    opens projectjava to javafx.graphics;
}
