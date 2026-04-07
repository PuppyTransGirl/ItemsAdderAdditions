export function VideoGif({ src, ...props }: { src: string } & React.VideoHTMLAttributes<HTMLVideoElement>) {
    return (
        <video autoPlay loop muted playsInline {...props}>
            <source src={`${src}.mp4`} type="video/mp4" />
        </video>
    );
}