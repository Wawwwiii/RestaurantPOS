module projectjava {
    requires javafx.controls;
    requires javafx.media;
    requires java.desktop;
    requires java.logging;
    opens projectjava to javafx.graphics;
}
