import { HomeBanner } from "@/components/home/home-banner";
import { HomeHeader } from "@/components/home/home-header";
import { HomeInfo } from "@/components/home/home-info";

export const Home = () => {
  return (
    <div>
      <HomeHeader />
      <HomeBanner />
      <HomeInfo />
    </div>
  );
};