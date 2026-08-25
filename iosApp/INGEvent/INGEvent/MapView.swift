import SwiftUI
import UIKit
import shared

struct MapView: View {
    @StateObject private var schedule = ScheduleViewModelWrapper()

    var body: some View {
        VStack(spacing: 0) {
            EventTopBar(section: "Map")

            Group {
                if schedule.state.isMapLoading {
                    ProgressView()
                        .tint(.ingOrange)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let image = decodedImage {
                    ZoomableMapImage(image: Image(uiImage: image))
                        .id(schedule.state.mapContent.imageBase64)
                } else if let url = remoteImageUrl {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            ZoomableMapImage(image: image)
                                .id(url)
                        case .failure:
                            mapMessage("Unable to display the event map.", showsRetry: true)
                        default:
                            ProgressView()
                                .tint(.ingOrange)
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                        }
                    }
                } else if let error = schedule.state.mapError {
                    mapMessage(error, showsRetry: true)
                } else {
                    mapMessage("The event map is not available yet.")
                }
            }
            .background(Color.vibrantBackground)
        }
        .onAppear {
            schedule.viewModel.loadMapContent()
        }
    }

    private var decodedImage: UIImage? {
        let cleanBase64 = schedule.state.mapContent.imageBase64
            .components(separatedBy: "base64,")
            .last?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !cleanBase64.isEmpty, let data = Data(base64Encoded: cleanBase64) else { return nil }
        return UIImage(data: data)
    }

    private var remoteImageUrl: URL? {
        let imageUrl = schedule.state.mapContent.imageUrl
        return imageUrl.isEmpty ? nil : URL(string: imageUrl)
    }

    @ViewBuilder
    private func mapMessage(_ message: String, showsRetry: Bool = false) -> some View {
        VStack(spacing: 16) {
            Text(message)
                .font(.body)
                .foregroundStyle(Color.vibrantMuted)
                .multilineTextAlignment(.center)
            if showsRetry {
                Button("Try again") {
                    schedule.viewModel.loadMapContent()
                }
                .buttonStyle(.borderedProminent)
                .tint(.ingOrange)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct ZoomableMapImage: View {
    let image: Image

    @State private var scale: CGFloat = 1
    @State private var offset: CGSize = .zero
    @GestureState private var gestureScale: CGFloat = 1
    @GestureState private var gestureOffset: CGSize = .zero

    var body: some View {
        GeometryReader { proxy in
            let currentScale = clampedScale(scale * gestureScale)
            let currentOffset = clampedOffset(
                CGSize(
                    width: offset.width + gestureOffset.width,
                    height: offset.height + gestureOffset.height
                ),
                viewport: proxy.size,
                scale: currentScale
            )

            image
                .resizable()
                .scaledToFit()
                .frame(width: proxy.size.width, height: proxy.size.height)
                .scaleEffect(currentScale)
                .offset(currentOffset)
                .contentShape(Rectangle())
                .gesture(
                    SimultaneousGesture(
                        magnificationGesture(viewport: proxy.size),
                        dragGesture(viewport: proxy.size)
                    )
                )
                .onTapGesture(count: 2) {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        if scale > 1 {
                            scale = 1
                            offset = .zero
                        } else {
                            scale = 2.5
                            offset = .zero
                        }
                    }
                }
                .accessibilityLabel("Event map")
                .accessibilityHint("Pinch to zoom and drag to move around the map")
        }
        .clipped()
    }

    private func magnificationGesture(viewport: CGSize) -> some Gesture {
        MagnificationGesture()
            .updating($gestureScale) { value, state, _ in
                state = value
            }
            .onEnded { value in
                scale = clampedScale(scale * value)
                offset = clampedOffset(offset, viewport: viewport, scale: scale)
            }
    }

    private func dragGesture(viewport: CGSize) -> some Gesture {
        DragGesture(minimumDistance: 1)
            .updating($gestureOffset) { value, state, _ in
                state = value.translation
            }
            .onEnded { value in
                offset = clampedOffset(
                    CGSize(
                        width: offset.width + value.translation.width,
                        height: offset.height + value.translation.height
                    ),
                    viewport: viewport,
                    scale: scale
                )
            }
    }

    private func clampedScale(_ proposedScale: CGFloat) -> CGFloat {
        min(max(proposedScale, 1), 5)
    }

    private func clampedOffset(_ proposedOffset: CGSize, viewport: CGSize, scale: CGFloat) -> CGSize {
        guard scale > 1 else { return .zero }

        let maxX = viewport.width * (scale - 1) / 2
        let maxY = viewport.height * (scale - 1) / 2
        return CGSize(
            width: min(max(proposedOffset.width, -maxX), maxX),
            height: min(max(proposedOffset.height, -maxY), maxY)
        )
    }
}
