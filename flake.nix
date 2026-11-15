{
    inputs.nixpkgs.url = "https://channels.nixos.org/nixos-unstable/nixexprs.tar.zst";

    outputs = { self, nixpkgs }: let
        systems = [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" ];
        forAllSystems = f: nixpkgs.lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system});
    in {
        devShells = forAllSystems (pkgs: let
            libs = builtins.attrValues {
                inherit
                    (pkgs)
                    libGL
                    glfw3-minecraft
                    libpulseaudio
                    ;
            };
        in {
            default = pkgs.mkShellNoCC {
                packages = [ pkgs.jdk25 pkgs.jdt-language-server ];

                env = {
                    LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath libs;
                    JAVA_HOME = "${pkgs.jdk25}/lib/openjdk/";
                };
            };
        });
    };
}
