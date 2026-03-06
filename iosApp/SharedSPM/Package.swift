// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SharedKMM",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "SharedKMM",
            targets: ["shared"]
        )
    ],
    targets: [
        .binaryTarget(
            name: "shared",
            path: "./shared.xcframework"
        )
    ]
)
