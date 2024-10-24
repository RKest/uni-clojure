{
  description = "A very basic flake";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
  };

  outputs = { self, nixpkgs }:
  let
     system = "x86_64-linux";
     pkgs = nixpkgs.legacyPackages."${system}";
  in
  {
    devShells.${system}.default = pkgs.stdenv.mkDerivation {
      name = "clojure-devshell";
      nativeBuildInputs = [pkgs.clojure pkgs.clojure-lsp pkgs.leiningen];
    };
  };
}
