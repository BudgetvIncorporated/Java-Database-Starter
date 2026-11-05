const base = import.meta.env.BASE_URL;

export default function Slide1() {
  return (
    <div className="w-screen h-screen overflow-hidden relative deck-frame">
      <div className="absolute inset-y-0 right-0 w-[54vw] overflow-hidden">
        <img
          src={`${base}game-library-cover.png`}
          crossOrigin="anonymous"
          className="h-full w-full object-cover object-center"
          alt="Illustrated desktop game library with linked library cards"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-[#f2f6ec] via-[#f2f6ec]/35 to-transparent" />
      </div>
      <div className="absolute left-[7vw] top-[16vh] z-10 w-[53vw]" data-layout-content="cover copy">
        <div className="mb-[3vh] h-[1.2vh] w-[8vw] rounded-full bg-[#b9df74]" />
        <h1 className="max-w-[50vw] font-display text-[6vw] font-bold leading-[.98] tracking-[-.06em] text-[#183329]">
          Java SQLite Game Library
        </h1>
        <p className="mt-[4vh] max-w-[43vw] text-[2.35vw] leading-[1.22] text-[#286747]">
          A desktop app for browsing games and managing a personal library
        </p>
        <p className="mt-[2.4vh] font-display text-[1.9vw] font-semibold text-[#183329]">
          Java Swing • SQLite • JDBC
        </p>
      </div>
      <div className="absolute bottom-[6vh] left-[7vw] h-[.8vh] w-[17vw] rounded-full bg-[#286747]" />
    </div>
  );
}
