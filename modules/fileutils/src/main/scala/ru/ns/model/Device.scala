package ru.ns.model

/**
  * Operation system device
  * <p>
  * Nix: "lsblk -Jf"
  * <p>
  * Win: FileStore
  *
  * @param name       - from lsblk in Nix
  * @param label      - from lsblk in Nix OR FileStore.name in Win
  * @param uuid       - from lsblk in Nix
  * @param mountpoint - mount point. In Nix "/home", in Win "C:"
  * @param fstype     - from lsblk in Nix
  */
case class Device(name: String,
                  label: String,
                  uuid: String,
                  mountpoint: String,
                  fstype: String
                 )
