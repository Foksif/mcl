{pkgs ? import <nixpkgs> {}}:
pkgs.mkShell {
  # Программы, которые должны быть доступны в PATH
  nativeBuildInputs = with pkgs; [
    openjdk25
    git
  ];

  # Библиотеки, которые нужны Minecraft для работы графики и звука
  buildInputs = with pkgs; [
    libGL
    glfw
    openal
    udev
    xorg.libX11
    xorg.libXcursor
    xorg.libXrandr
    xorg.libXinerama
    xorg.libXxf86vm
    flite
    vulkan-loader
    libpulseaudio
  ];

  shellHook = ''
    # Формируем путь к библиотекам для загрузчика
    export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath (with pkgs; [
      libGL
      glfw
      openal
      udev
      stdenv.cc.cc.lib
      xorg.libX11
      xorg.libXcursor
      xorg.libXrandr
      xorg.libXinerama
      xorg.libXxf86vm
      flite
      vulkan-loader
      libpulseaudio
    ])}:$LD_LIBRARY_PATH"

    # Если у вас NVIDIA, раскомментируйте следующую строку:
    # export LD_LIBRARY_PATH="/run/opengl-driver/lib:/run/opengl-driver-32/lib:$LD_LIBRARY_PATH"

    echo "--- Fabric Development Shell ---"
    echo "Java version: $(java -version 2>&1 | head -n 1)"
    echo "Ready to run: ./gradlew runClient"
  '';
}
