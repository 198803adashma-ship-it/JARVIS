from kivy.app import App
from kivy.uix.label import Label


class JarvisApp(App):
    def build(self):
        return Label(
            text="JARVIS ONLINE",
            font_size="32sp"
        )


if __name__ == "__main__":
    JarvisApp().run()
