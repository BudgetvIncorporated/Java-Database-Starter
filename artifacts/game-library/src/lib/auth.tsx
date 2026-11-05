import { SignIn, SignUp } from "@clerk/react";

const basePath = import.meta.env.BASE_URL.replace(/\/$/, "");

export const clerkAppearance = {
  cssLayerName: "clerk",
  variables: {
    colorPrimary: "#355b4b",
    colorForeground: "#242b28",
    colorMutedForeground: "#626b65",
    colorDanger: "#a12727",
    colorBackground: "#fffdf7",
    colorInput: "#ffffff",
    colorInputForeground: "#242b28",
    colorNeutral: "#242b28",
    fontFamily: "inherit",
    borderRadius: "6px",
  },
  elements: {
    rootBox: "w-full flex justify-center",
    cardBox: "bg-[#fffdf7] rounded-md w-[440px] max-w-full overflow-hidden",
    card: "!shadow-none !border-0 !bg-transparent !rounded-none",
    footer: "!shadow-none !border-0 !bg-transparent !rounded-none",
    headerTitle: "text-[#242b28] font-bold",
    headerSubtitle: "text-[#626b65]",
    socialButtonsBlockButtonText: "text-[#242b28]",
    formFieldLabel: "text-[#242b28]",
    footerActionLink: "text-[#355b4b]",
    footerActionText: "text-[#626b65]",
    dividerText: "text-[#626b65]",
    identityPreviewEditButton: "text-[#355b4b]",
    formFieldSuccessText: "text-[#355b4b]",
    alertText: "text-[#a12727]",
    logoBox: "mx-auto",
    logoImage: "max-h-12",
    socialButtonsBlockButton: "border border-[#626b65] bg-white",
    formButtonPrimary: "bg-[#355b4b] text-white",
    formFieldInput: "bg-white text-[#242b28] border-[#626b65]",
    footerAction: "bg-transparent",
    dividerLine: "bg-[#626b65]",
    alert: "bg-red-50",
    otpCodeFieldInput: "bg-white text-[#242b28]",
    formFieldRow: "gap-2",
    main: "gap-4",
  },
};

export function SignInPage() {
  return <div className="py-8"><SignIn routing="path" path={`${basePath}/sign-in`} signUpUrl={`${basePath}/sign-up`} fallbackRedirectUrl={basePath || "/"} /></div>;
}
export function SignUpPage() {
  return <div className="py-8"><SignUp routing="path" path={`${basePath}/sign-up`} signInUrl={`${basePath}/sign-in`} fallbackRedirectUrl={basePath || "/"} /></div>;
}
