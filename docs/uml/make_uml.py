import sys
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle, Polygon, Circle, FancyBboxPatch
from matplotlib import rcParams
from pathlib import Path

rcParams["font.family"] = "Arial"
OUT = Path(r"C:\Users\madha\OneDrive\Desktop\uni\oops\urbanflow\docs\uml")
OUT.mkdir(parents=True, exist_ok=True)
INK = "#1E3A5F"; FILL = "#EEF4FB"; FILL_I = "#FFF4E0"; FILL_E = "#EAF7EE"; FILL_X = "#FBECEC"
NOTE = "#5A6B80"; FS = 9; LH = 0.22
ONLY = sys.argv[1:]


def want(name):
    return not ONLY or name in ONLY


def canvas(w, h):
    fig = plt.figure(figsize=(w, h), dpi=200)
    ax = fig.add_axes([0, 0, 1, 1]); ax.set_xlim(0, w); ax.set_ylim(0, h); ax.axis("off")
    ax.add_patch(Rectangle((0, 0), w, h, fc="white", ec="none", zorder=0))
    return fig, ax


def unit(p, q):
    v = np.array(q, float) - np.array(p, float)
    return v / np.linalg.norm(v)


class Box:
    def __init__(s, ax, cx, top, w, name, attrs=None, ops=None, stereo=None, abstract=False, fill=FILL, fs=FS):
        hh = 0.34 + (0.17 if stereo else 0)
        ah = (max(len(attrs), 1) * LH + 0.1) if attrs is not None else 0
        oh = (max(len(ops), 1) * LH + 0.1) if ops is not None else 0
        s.w, s.h, s.cx, s.top = w, hh + ah + oh, cx, top
        s.x, s.bottom = cx - w / 2, top - s.h
        ax.add_patch(Rectangle((s.x, s.bottom), w, s.h, fc=fill, ec=INK, lw=1.2, zorder=2))
        if stereo:
            ax.text(cx, top - 0.13, "\u00ab" + stereo + "\u00bb", ha="center", va="center",
                    fontsize=fs - 1.5, color=INK, zorder=3)
        ax.text(cx, top - hh + 0.17, name, ha="center", va="center", fontsize=fs + 1, fontweight="bold",
                fontstyle="italic" if abstract else "normal", color=INK, zorder=3)
        y = top - hh
        for items, hgt in ((attrs, ah), (ops, oh)):
            if items is None:
                continue
            ax.plot([s.x, s.x + w], [y, y], color=INK, lw=1.0, zorder=3)
            for i, t in enumerate(items):
                it = False
                if isinstance(t, tuple):
                    t, it = t
                ax.text(s.x + 0.07, y - 0.05 - LH * (i + 0.5), t, ha="left", va="center", fontsize=fs - 1.2,
                        fontstyle="italic" if it else "normal", color="#111", zorder=3)
            y -= hgt

    def T(s, f=0.5): return (s.x + s.w * f, s.top)
    def B(s, f=0.5): return (s.x + s.w * f, s.bottom)
    def L(s, f=0.5): return (s.x, s.top - s.h * f)
    def R(s, f=0.5): return (s.x + s.w, s.top - s.h * f)
    def Ly(s, y): return (s.x, y)
    def Ry(s, y): return (s.x + s.w, y)
    def Tx(s, x): return (x, s.top)
    def Bx(s, x): return (x, s.bottom)


def tri(ax, tip, frm, L=0.2, Wd=0.11):
    d = unit(frm, tip); n = np.array([-d[1], d[0]]); base = np.array(tip) - d * L
    ax.add_patch(Polygon([tip, base + n * Wd, base - n * Wd], closed=True, fc="white", ec=INK, lw=1.2, zorder=4))
    return tuple(base)


def diamond(ax, tip, frm, filled, L=0.3, Wd=0.08):
    d = unit(frm, tip); n = np.array([-d[1], d[0]]); mid = np.array(tip) - d * L / 2; end = np.array(tip) - d * L
    ax.add_patch(Polygon([tip, mid + n * Wd, end, mid - n * Wd], closed=True,
                         fc=INK if filled else "white", ec=INK, lw=1.2, zorder=4))
    return tuple(end)


def open_arrow(ax, tip, frm, L=0.15, Wd=0.07, color=INK):
    d = unit(frm, tip); n = np.array([-d[1], d[0]]); b = np.array(tip) - d * L
    ax.plot([b[0] + n[0] * Wd, tip[0], b[0] - n[0] * Wd], [b[1] + n[1] * Wd, tip[1], b[1] - n[1] * Wd],
            color=color, lw=1.2, zorder=4, solid_joinstyle="miter")


def solid_arrow(ax, tip, frm, L=0.16, Wd=0.055, color=INK):
    d = unit(frm, tip); n = np.array([-d[1], d[0]]); b = np.array(tip) - d * L
    ax.add_patch(Polygon([tip, b + n * Wd, b - n * Wd], closed=True, fc=color, ec=color, lw=1, zorder=4))


def seg(ax, p, q, dashed=False, lw=1.2, color=INK, z=1):
    ax.plot([p[0], q[0]], [p[1], q[1]], color=color, lw=lw, ls=(0, (4, 3)) if dashed else "-",
            zorder=z, solid_capstyle="butt")


def path(ax, pts, kind):
    """pts[0] = source (whole end for diamonds), pts[-1] = target."""
    pts = [tuple(p) for p in pts]
    dashed = kind in ("real", "dep")
    if kind in ("comp", "agg"):
        pts[0] = diamond(ax, pts[0], pts[1], kind == "comp")
    if kind in ("gen", "real"):
        pts[-1] = tri(ax, pts[-1], pts[-2])
    for a, b in zip(pts, pts[1:]):
        seg(ax, a, b, dashed)
    if kind in ("assoc_dir", "dep"):
        open_arrow(ax, pts[-1], pts[-2])


def txt(ax, x, y, s, size=FS - 1, ha="center", va="center", color=NOTE, **kw):
    ax.text(x, y, s, fontsize=size, ha=ha, va=va, color=color, zorder=5, **kw)


def lollipop(ax, start, end, label, ha="center", dx=0.0, dy=0.2):
    seg(ax, start, end)
    ax.add_patch(Circle(end, 0.08, fc="white", ec=INK, lw=1.2, zorder=4))
    txt(ax, end[0] + dx, end[1] + dy, label, size=8, color=INK, ha=ha)


def package(ax, x, y, w, h, label):
    ax.add_patch(Rectangle((x, y), w, h, fc="none", ec="#9AAFC7", lw=1.0, ls=(0, (5, 3)), zorder=0.5))
    ax.add_patch(Rectangle((x, y + h), 1.25, 0.24, fc="#E3ECF6", ec="#9AAFC7", lw=1.0, zorder=0.5))
    txt(ax, x + 0.08, y + h + 0.12, label, size=8.5, ha="left", color=INK, fontweight="bold")


def title(ax, w, h, s):
    txt(ax, w / 2, h - 0.2, s, size=11.5, color=INK, fontweight="bold")


# ======================================================================= 1. architecture
if want("1"):
    W, H = 11.6, 9.9
    fig, ax = canvas(W, H)
    title(ax, W, H, "Figure 1. UrbanFlow architecture: controller, event bus and subsystems")
    t1 = 9.05
    clock = Box(ax, 1.75, t1, 2.9, "SimulationClock",
                ["- minuteOfDay : int", "- simMinutesPerSecond : int"],
                ["+ run() : void", "+ isNight() : boolean", "+ isPeakHour() : boolean", "+ timeString() : String"])
    cc = Box(ax, 5.8, t1, 3.6, "CityController",
             ["- instance : CityController  {static}", "- subsystems : ArrayList<Subsystem>"],
             ["+ getInstance() : CityController  {static}", "+ start() : void", "+ dispatchAmbulance() : void",
              "+ reportAccident(area : String) : void", "+ toggleSignalMode() : String",
              "+ endOfDayReport() : String"], stereo="singleton")
    bus = Box(ax, 9.85, t1, 3.2, "EventBus",
              ["- listeners : HashMap<EventType, ArrayList<..>>", "- queue : LinkedList<CityEvent>"],
              ["+ subscribe(l, types : EventType...) : void", "+ publish(e : CityEvent) : void",
               "+ run() : void   // wait() / notify()"])
    lollipop(ax, clock.T(0.85), (clock.x + clock.w * 0.85, t1 + 0.35), "Runnable", ha="left", dx=0.15, dy=0.0)
    lollipop(ax, bus.T(0.85), (bus.x + bus.w * 0.85, t1 + 0.35), "Runnable", ha="left", dx=0.15, dy=0.0)
    yc = 8.25
    path(ax, [cc.Ly(yc), clock.Ry(yc)], "comp")
    txt(ax, clock.x + clock.w + 0.1, yc + 0.14, "1", size=8.5, ha="left")
    path(ax, [cc.Ry(yc), bus.Ly(yc)], "comp")
    txt(ax, bus.x - 0.1, yc + 0.14, "1", size=8.5, ha="right")

    t2 = 6.0
    thread = Box(ax, 0.95, t2, 1.5, "Thread", None, ["+ start() : void", "+ run() : void", "+ join(ms : long)"],
                 stereo="java.lang", fill="#F3F3F3")
    sub = Box(ax, 3.55, t2, 2.6, "Subsystem", None,
              ["+ getName() : String", "+ start() : void", "+ shutdown() : void", "+ statusReport() : String"],
              stereo="interface", fill=FILL_I)
    lis = Box(ax, 7.45, t2, 2.7, "CityEventListener", None, ["+ onEvent(e : CityEvent) : void"],
              stereo="interface", fill=FILL_I)
    ev = Box(ax, 10.3, t2, 2.2, "CityEvent", ["- type : EventType", "- area : String", "- payload : Object"],
             ["+ getType() : EventType"])
    # controller -> subsystems
    x = 4.35
    path(ax, [(x, cc.bottom), (x, sub.top)], "assoc_dir")
    txt(ax, x + 0.1, sub.top + 0.15, "3", size=8.5, ha="left")
    txt(ax, x - 0.1, (cc.bottom + sub.top) / 2, "subsystems", size=8, ha="right")
    # bus -> listeners / events
    x = 8.5
    path(ax, [(x, bus.bottom), (x, lis.top)], "assoc_dir")
    txt(ax, x + 0.1, lis.top + 0.15, "0..*", size=8.5, ha="left")
    txt(ax, x - 0.1, (bus.bottom + lis.top) / 2, "listeners", size=8, ha="right")
    x = 10.6
    path(ax, [(x, bus.bottom), (x, ev.top)], "assoc_dir")
    txt(ax, x + 0.1, ev.top + 0.15, "0..*", size=8.5, ha="left")
    txt(ax, x - 0.1, (bus.bottom + ev.top) / 2, "queue", size=8, ha="right")
    path(ax, [lis.Ry(5.35), ev.Ly(5.35)], "dep")
    txt(ax, (lis.x + lis.w + ev.x) / 2, 5.5, "uses", size=8)

    t3 = 3.85
    ab = Box(ax, 4.9, t3, 3.4, "AbstractSubsystem",
             ["# bus : EventBus", "# clock : SimulationClock", "- running : boolean  {volatile}"],
             [("# tick() : void  {abstract}", True), "+ run() : void  {final}", "+ shutdown() : void"], abstract=True)
    path(ax, [ab.Tx(3.9), (3.9, sub.bottom)], "real")
    path(ax, [ab.Ry(3.2), (7.45, 3.2), (7.45, lis.bottom)], "real")
    path(ax, [ab.Ly(3.2), (0.95, 3.2), (0.95, thread.bottom)], "gen")
    txt(ax, 1.05, 3.35, "extends", size=8, ha="left")
    txt(ax, 7.55, 3.35, "implements", size=8, ha="left")
    txt(ax, 3.8, 4.2, "implements", size=8, ha="right")
    txt(ax, ab.x + ab.w + 0.15, ab.bottom + 0.15, "Template Method:\nrun() loops tick() + sleep()", size=8, ha="left")

    t4 = 1.3
    kids = [Box(ax, 2.2, t4, 2.5, "TrafficSubsystem", None, ["# tick() : void", "+ onEvent(e) : void"]),
            Box(ax, 4.9, t4, 2.5, "LightingSubsystem", None, ["# tick() : void", "+ onEvent(e) : void"]),
            Box(ax, 7.6, t4, 2.5, "ParkingSubsystem", None, ["# tick() : void", "+ onEvent(e) : void"])]
    ys = kids[0].top
    bus_y = (ab.bottom + ys) / 2
    base = tri(ax, ab.B(), (ab.cx, bus_y))
    seg(ax, base, (ab.cx, bus_y)); seg(ax, (kids[0].cx, bus_y), (kids[-1].cx, bus_y))
    for k in kids:
        seg(ax, (k.cx, bus_y), k.T())
    txt(ax, 9.0, 0.95, "each subsystem is its own thread\nand subscribes to the events it needs",
        size=8, ha="left")
    fig.savefig(OUT / "fig1_architecture.png"); plt.close(fig)


# ======================================================================= 2. subsystem internals
if want("2"):
    W, H = 12.5, 9.0
    fig, ax = canvas(W, H)
    title(ax, W, H, "Figure 2. Inside the three subsystems")
    package(ax, 0.1, 0.15, 3.75, 8.2, "traffic")
    package(ax, 4.0, 0.15, 3.75, 8.2, "lighting")
    package(ax, 7.9, 0.15, 4.5, 8.2, "parking")
    top = 8.05
    # ---- traffic
    ts = Box(ax, 1.95, top, 3.5, "TrafficSubsystem",
             ["- intersections : ArrayList<Intersection>", "- currentStrategy : SignalStrategy",
              "- fixedStats, adaptiveStats : WaitStats"],
             ["+ dispatchAmbulance(a : Ambulance) : void", "+ toggleSignalMode() : String", "# tick() : void"])
    it = Box(ax, 1.95, 5.75, 3.5, "Intersection",
             ["- queues : HashMap<Direction, LinkedList<Vehicle>>", "- blocked : boolean", "- emergencyRequests : int"],
             ["+ arrive(v, from) : void", "    throws RoadBlockedException", "+ preempt(dir : Direction) : void",
              "+ step() : ArrayList<Vehicle>"])
    ss = Box(ax, 1.95, 3.2, 3.2, "SignalStrategy", None,
             ["+ chooseNextGreen(..) : Direction", "+ greenTimeFor(..) : int", "+ endsGreenEarly() : boolean"],
             stereo="interface", fill=FILL_I)
    fx = Box(ax, 1.0, 1.25, 1.75, "FixedTimerStrategy", ["- greenSeconds : int"], fs=8)
    ad = Box(ax, 2.9, 1.25, 1.75, "AdaptiveStrategy", ["- minGreen, maxGreen"], fs=8)
    path(ax, [ts.B(), it.T()], "comp")
    txt(ax, 2.05, it.top + 0.15, "3", size=8.5, ha="left")
    path(ax, [it.B(), ss.T()], "assoc_dir")
    txt(ax, 2.05, ss.top + 0.15, "1", size=8.5, ha="left"); txt(ax, 1.85, (it.bottom + ss.top) / 2, "strategy", size=8, ha="right")
    by = (ss.bottom + fx.top) / 2
    base = tri(ax, ss.B(), (ss.cx, by)); seg(ax, base, (ss.cx, by), True)
    seg(ax, (fx.cx, by), (ad.cx, by), True)
    for k in (fx, ad):
        seg(ax, (k.cx, by), k.T(), True)
    txt(ax, 1.95, 0.35, "Strategy pattern: swap at runtime", size=8)

    # ---- lighting
    ls = Box(ax, 5.85, top, 3.5, "LightingSubsystem",
             ["- lights : ArrayList<StreetLight>", "- accidentAreas : HashSet<String>",
              "- ambulanceTicks : HashMap<String, Integer>"],
             ["# tick() : void", "+ onEvent(e : CityEvent) : void"])
    sl = Box(ax, 5.85, 5.75, 3.2, "StreetLight",
             ["- mode : LightMode", "- energyWh : double", "- brightTicksLeft : int"],
             ["+ applyMode(m : LightMode) : void", "+ vehicleNearby() : void", "+ tick(h, night) : boolean",
              "+ repair() : void"])
    rc = Box(ax, 5.85, 3.2, 3.2, "RepairCrew",
             ["- jobs : LinkedList<StreetLight>"],
             ["+ reportFault(l : StreetLight) : void", "+ run() : void   // wait()"])
    lm = Box(ax, 5.85, 1.35, 3.0, "LightMode", ["OFF, DIM, FULL, FLASHING, FAULTY"], ["+ getBrightness() : int"],
             stereo="enumeration", fill=FILL_E)
    path(ax, [ls.B(), sl.T()], "comp")
    txt(ax, 5.95, sl.top + 0.15, "9", size=8.5, ha="left")
    path(ax, [ls.Ry(7.0), (7.6, 7.0), (7.6, 2.8), rc.Ry(2.8)], "comp")
    txt(ax, rc.x + rc.w + 0.05, 2.95, "1", size=8.5, ha="left")
    path(ax, [rc.T(), sl.B()], "assoc_dir")
    txt(ax, 5.95, sl.bottom - 0.15, "0..*", size=8.5, ha="left"); txt(ax, 5.75, (rc.top + sl.bottom) / 2, "jobs", size=8, ha="right")
    path(ax, [sl.Ly(4.2), (4.15, 4.2), (4.15, 1.0), lm.Ly(1.0)], "assoc_dir")
    lollipop(ax, rc.Bx(7.05), (7.05, rc.bottom - 0.3), "Runnable", ha="left", dx=0.14, dy=0.0)

    # ---- parking
    ps = Box(ax, 10.15, top, 3.5, "ParkingSubsystem",
             ["- lots : ArrayList<ParkingLot>", "- reroutes : int"],
             ["- findParking(v, area) : void", "- parkAmbulance(a : Ambulance) : void", "# tick() : void"])
    pl = Box(ax, 10.15, 5.75, 3.6, "ParkingLot",
             ["- slots : ArrayList<ParkingSlot>", "- waiting : PriorityQueue<Vehicle>", "- closed : boolean"],
             ["+ park(v : Vehicle) : void", "    throws ParkingFullException,", "              LotClosedException",
              "+ tick(peak, minutes) : void"])
    sl2 = Box(ax, 9.05, 3.2, 1.95, "ParkingSlot", ["- occupant : Vehicle", "- type : SlotType"],
              ["+ occupy(v) : void"], fs=8)
    pr = Box(ax, 11.3, 3.2, 1.95, "PricingStrategy", None, ["+ pricePerHour(..)"], stereo="interface",
             fill=FILL_I, fs=8)
    dp = Box(ax, 11.3, 1.55, 1.95, "DynamicPricing", None, ["+ pricePerHour(..)"], fs=8)
    st = Box(ax, 9.05, 1.6, 1.95, "SlotType", ["BIKE, STANDARD,", "EV_CHARGING,", "HEAVY, EMERGENCY"],
             stereo="enumeration", fill=FILL_E, fs=8)
    path(ax, [ps.B(), pl.T()], "comp")
    txt(ax, 10.25, pl.top + 0.15, "2", size=8.5, ha="left")
    path(ax, [pl.Bx(9.05), sl2.T()], "comp")
    txt(ax, 9.15, sl2.top + 0.15, "1..*", size=8.5, ha="left")
    path(ax, [pl.Bx(11.3), pr.T()], "assoc_dir")
    txt(ax, 11.4, pr.top + 0.15, "1", size=8.5, ha="left")
    path(ax, [dp.T(), pr.B()], "real")
    path(ax, [sl2.B(), st.T()], "assoc_dir")
    fig.savefig(OUT / "fig2_subsystems.png"); plt.close(fig)


# ======================================================================= 3. hierarchies
if want("3"):
    W, H = 11.8, 6.4
    fig, ax = canvas(W, H)
    title(ax, W, H, "Figure 3. Vehicle hierarchy and custom exception hierarchy")
    package(ax, 0.1, 0.15, 6.95, 5.65, "model")
    package(ax, 7.2, 0.15, 4.5, 5.65, "exceptions")
    v = Box(ax, 2.65, 5.55, 3.2, "Vehicle",
            ["- id : String", "- type : VehicleType", "- nextNumber : int  {static}"],
            [("+ getPriority() : int  {abstract}", True), "+ isEmergency() : boolean", "+ compareTo(o : Vehicle) : int"],
            abstract=True)
    lollipop(ax, v.Ly(4.6), (0.5, 4.6), "Comparable\n<Vehicle>", dy=0.32)
    vf = Box(ax, 5.95, 5.55, 2.1, "VehicleFactory", None, ["+ randomVehicle() : Vehicle", "      {static}"])
    path(ax, [vf.Ly(5.0), v.Ry(5.0)], "dep")
    txt(ax, (v.x + v.w + vf.x) / 2, 5.17, "\u00abcreates\u00bb", size=8)
    ky = 3.05
    car = Box(ax, 0.95, ky, 1.55, "Car", None, ["+ Car()", "# Car(type, prefix)"], fs=8.5)
    bike = Box(ax, 2.6, ky, 1.55, "Bike", None, ["+ getPriority() : int"], fs=8.5)
    truck = Box(ax, 4.25, ky, 1.55, "Truck", None, ["+ getPriority() : int"], fs=8.5)
    amb = Box(ax, 6.0, ky, 1.75, "Ambulance", ["- routeIndex : int"], ["+ isEmergency() : boolean"], fs=8.5)
    kids = [car, bike, truck, amb]
    by = (v.bottom + ky) / 2
    base = tri(ax, v.B(), (v.cx, by)); seg(ax, base, (v.cx, by))
    seg(ax, (car.cx, by), (amb.cx, by))
    for k in kids:
        seg(ax, (k.cx, by), k.T())
    txt(ax, 3.6, by + 0.14, "hierarchical inheritance", size=8, ha="left")
    ec = Box(ax, 0.95, 1.35, 1.6, "ElectricCar", ["- battery : int"], ["+ getPriority() : int"], fs=8.5)
    path(ax, [ec.T(), car.B()], "gen")
    txt(ax, 1.05, (car.bottom + ec.top) / 2, "multilevel", size=8, ha="left")
    ch = Box(ax, 3.55, 1.45, 2.7, "Chargeable", None,
             ["+ getBatteryPercent() : int", "+ charge(p : int) : void", ("+ isFullyCharged() : boolean  {default}", True)],
             stereo="interface", fill=FILL_I, fs=8.5)
    path(ax, [ec.Ry(0.85), ch.Ly(0.85)], "real")
    txt(ax, 5.05, 0.45, "ElectricCar extends Car\nimplements Chargeable", size=8, ha="left")

    ex = Box(ax, 9.45, 5.55, 2.0, "Exception", None, None, stereo="java.lang", fill="#F3F3F3")
    ce = Box(ax, 9.45, 4.55, 2.9, "CityException", None, ["+ CityException(msg : String)"], fill=FILL_X)
    path(ax, [ce.T(), ex.B()], "gen")
    txt(ax, 9.55, (ex.bottom + ce.top) / 2, "checked", size=8, ha="left")
    names = [("ParkingFullException", "ParkingLot.park()"), ("LotClosedException", "ParkingLot.park()"),
             ("SlotAlreadyTakenException", "ParkingSlot.occupy()"), ("RoadBlockedException", "Intersection.arrive()")]
    pos = [(8.3, 3.15), (10.6, 3.15), (8.3, 1.55), (10.6, 1.55)]
    trunk_x = 9.45
    kids = []
    for (n, by_), (cx, tp) in zip(names, pos):
        kids.append(Box(ax, cx, tp, 2.0, n, None, ["thrown by " + by_], fill=FILL_X, fs=7.4))
    base = tri(ax, ce.B(), (trunk_x, ce.bottom - 0.4))
    low = kids[2].top - kids[2].h / 2
    seg(ax, base, (trunk_x, low))
    for k in kids:
        y = k.top - k.h / 2
        seg(ax, (trunk_x, y), k.Ry(y) if k.cx < trunk_x else k.Ly(y))
    txt(ax, 9.45, 0.45, "caught with try/catch (incl. multi-catch)\nin ParkingSubsystem and TrafficSubsystem",
        size=8)
    fig.savefig(OUT / "fig3_hierarchies.png"); plt.close(fig)


# ======================================================================= sequence helpers
class Seq:
    def __init__(s, w, h, names, xs, bw=1.35, head=None):
        s.fig, s.ax = canvas(w, h)
        s.xs = xs
        tb = h - 0.45
        s.tb = tb
        for n, x in zip(names, xs):
            s.ax.add_patch(Rectangle((x - bw / 2, tb - 0.42), bw, 0.42, fc=FILL, ec=INK, lw=1.2, zorder=2))
            parts = n.split("\n")
            if len(parts) == 1:
                txt(s.ax, x, tb - 0.21, n, size=8.3, color=INK, fontweight="bold")
            else:
                txt(s.ax, x, tb - 0.12, parts[0], size=7.6, color=INK)
                txt(s.ax, x, tb - 0.3, parts[1], size=8.2, color=INK, fontweight="bold")
            seg(s.ax, (x, tb - 0.42), (x, 0.15), dashed=True, lw=0.9, color="#7A8CA3")
        if head:
            title(s.ax, w, h, head)

    def act(s, i, y1, y2):
        x = s.xs[i]
        s.ax.add_patch(Rectangle((x - 0.06, y2), 0.12, y1 - y2, fc="white", ec=INK, lw=1.0, zorder=3))

    def msg(s, i, j, y, label, ret=False, color=INK, lab_color="#111", align="mid"):
        x1, x2 = s.xs[i], s.xs[j]
        a = x1 + (0.06 if x2 > x1 else -0.06); b = x2 - (0.06 if x2 > x1 else -0.06)
        seg(s.ax, (a, y), (b, y), dashed=ret, color=color, z=3)
        if ret:
            open_arrow(s.ax, (b, y), (a, y), color=color)
        else:
            solid_arrow(s.ax, (b, y), (a, y), color=color)
        if align == "mid" and len(label) * 0.062 > abs(x2 - x1) - 0.35:
            align = "src"                    # long label: start it just past the sender's bar
        if align == "mid":
            txt(s.ax, (x1 + x2) / 2, y + 0.11, label, size=7.9, color=lab_color)
        elif align == "src":
            txt(s.ax, a + (0.16 if x2 > x1 else -0.16), y + 0.11, label, size=7.9, color=lab_color,
                ha="left" if x2 > x1 else "right")

    def self_msg(s, i, y, label):
        x = s.xs[i] + 0.06
        pts = [(x, y), (x + 0.35, y), (x + 0.35, y - 0.22), (x + 0.02, y - 0.22)]
        for a, b in zip(pts, pts[1:]):
            seg(s.ax, a, b, z=3)
        solid_arrow(s.ax, pts[-1], pts[-2])
        txt(s.ax, x + 0.42, y - 0.08, label, size=7.9, color="#111", ha="left")

    def frame(s, x0, x1, y0, y1, kind, guard):
        ax = s.ax
        ax.add_patch(Rectangle((x0, y1), x1 - x0, y0 - y1, fc="none", ec=INK, lw=1.1, zorder=4))
        ax.add_patch(Polygon([(x0, y0), (x0 + 0.5, y0), (x0 + 0.58, y0 - 0.1), (x0 + 0.58, y0 - 0.22),
                              (x0, y0 - 0.22)], closed=True, fc=FILL, ec=INK, lw=1.0, zorder=5))
        txt(ax, x0 + 0.25, y0 - 0.11, kind, size=8, color=INK, fontweight="bold")
        txt(ax, x0 + 0.68, y0 - 0.11, guard, size=7.9, ha="left", color=INK)

    def note(s, x, y, text, w):
        lines = text.count("\n") + 1
        h = 0.17 * lines + 0.12
        s.ax.add_patch(Polygon([(x, y), (x + w - 0.12, y), (x + w, y - 0.12), (x + w, y - h), (x, y - h)],
                               closed=True, fc="#FFFBE6", ec="#B59B3A", lw=0.9, zorder=5))
        txt(s.ax, x + 0.08, y - h / 2, text, size=7.8, ha="left", color="#4A4020")

    def save(s, name):
        s.fig.savefig(OUT / name); plt.close(s.fig)


# ======================================================================= 4. ambulance sequence
if want("4"):
    names = [":CityDashboard", ":CityController", ":TrafficSubsystem", "INT-k\n:Intersection", ":EventBus",
             ":LightingSubsystem", ":ParkingSubsystem", "Hospital Lot\n:ParkingLot"]
    xs = [0.8, 2.3, 3.85, 5.35, 6.75, 8.15, 9.6, 11.0]
    s = Seq(11.8, 8.4, names, xs, head="Figure 4. Sequence diagram: ambulance dispatch with green wave")
    y = 7.3
    s.act(1, y, y - 0.45); s.act(2, y - 0.3, y - 1.45)
    s.msg(0, 1, y, "1: dispatchAmbulance()")
    s.msg(1, 2, y - 0.3, "2: dispatchAmbulance(new Ambulance())")
    s.msg(2, 3, y - 0.65, "3: preempt(WEST)  [INT-1, INT-2]")
    s.msg(2, 4, y - 1.0, "4: publish(AMBULANCE_DISPATCHED)", align="src")
    s.act(4, y - 1.35, y - 1.65); s.act(5, y - 1.35, y - 1.65)
    s.msg(4, 5, y - 1.35, "5: onEvent(e)")
    txt(s.ax, xs[5] + 0.12, y - 1.6, "lights in West \u2192 FULL", size=7.6, ha="left", color="#8A5A00")
    # loop
    f0, f1 = 5.35, 2.3
    s.frame(2.95, 9.0, f0, f1, "loop", "[for each junction INT-1 \u2192 INT-3]")
    s.act(2, 4.95, 2.45)
    s.msg(2, 3, 4.95, "6: step()")
    s.act(3, 4.95, 4.62)
    s.msg(3, 2, 4.62, "7: passed list", ret=True)
    s.msg(2, 3, 4.25, "8: preempt(WEST)  [junction after next]")
    s.note(5.95, 4.15, "wave stays one junction\nahead of the ambulance", 1.9)
    s.msg(2, 3, 3.6, "9: arrive(amb, WEST)  after 4 ticks of driving")
    s.msg(2, 4, 3.15, "10: publish(AMBULANCE_MOVED)", align="src")
    s.act(5, 2.8, 2.5)
    s.msg(4, 5, 2.8, "11: onEvent(e)")
    txt(s.ax, xs[5] + 0.12, 2.55, "lights ahead \u2192 FULL", size=7.6, ha="left", color="#8A5A00")
    # arrival
    s.act(2, 1.95, 1.75)
    s.msg(2, 4, 1.95, "12: publish(AMBULANCE_ARRIVED)", align="src")
    s.act(6, 1.6, 0.75)
    s.msg(4, 6, 1.6, "13: onEvent(e)")
    s.act(7, 1.25, 1.0)
    s.msg(6, 7, 1.25, "14: park(amb)")
    txt(s.ax, xs[7] - 0.12, 1.07, "EMERGENCY bay", size=7.6, ha="right", color="#8A5A00")
    s.msg(6, 4, 0.75, "15: publish(AMBULANCE_PARKED)")
    s.note(0.15, 5.2, "Subsystems never call\neach other directly:\nthey publish events and\nthe EventBus thread\ndelivers them (Observer)", 2.05)
    s.save("fig4_seq_ambulance.png")


# ======================================================================= 5. parking sequence
if want("5"):
    names = [":TrafficSubsystem", ":EventBus", ":ParkingSubsystem", "Mall Lot\n:ParkingLot", "Hospital Lot\n:ParkingLot"]
    xs = [0.95, 2.6, 4.4, 6.35, 8.25]
    s = Seq(9.3, 6.4, names, xs, head="Figure 5. Sequence diagram: preferred lot full \u2192 car rerouted")
    y = 5.35
    s.act(0, y, y - 0.2)
    s.msg(0, 1, y, "1: publish(CAR_NEEDS_PARKING, car)")
    s.act(1, y - 0.35, y - 0.6)
    s.msg(1, 2, y - 0.35, "2: onEvent(e)")
    s.act(2, y - 0.35, 0.55)
    s.self_msg(2, y - 0.65, "3: findParking(car, \"West\")")
    s.act(3, 4.25, 3.95)
    s.msg(2, 3, 4.25, "4: park(car)")
    s.msg(3, 2, 3.95, "5: \u00abthrows\u00bb ParkingFullException", ret=True, color="#B03030", lab_color="#B03030")
    s.frame(3.6, 9.15, 3.65, 0.35, "alt", "[another lot has a free slot]")
    s.act(4, 3.2, 2.9)
    s.msg(2, 4, 3.2, "6: park(car)")
    s.msg(4, 2, 2.9, "7: return (parked)", ret=True)
    s.msg(2, 1, 2.5, "8: publish(CAR_REROUTED)")
    ye = 2.15
    seg(s.ax, (3.6, ye), (9.15, ye), dashed=True, lw=1.0, z=4)
    txt(s.ax, 3.68, ye - 0.13, "[all lots full]", size=7.9, ha="left", color=INK)
    s.act(3, 1.7, 1.45)
    s.msg(2, 3, 1.7, "9: addToWaiting(car)   (PriorityQueue)")
    txt(s.ax, 4.55, 1.05, "or give up if 8 cars are already waiting", size=7.6, ha="left", color="#8A5A00")
    s.note(0.15, 3.8, "The exception is caught\nin findParking() with\nmulti-catch:\ncatch (ParkingFullException\n     | LotClosedException e)", 2.3)
    s.save("fig5_seq_parking.png")


# ======================================================================= 6. state machine
if want("6"):
    W, H = 10.4, 5.9
    fig, ax = canvas(W, H)
    title(ax, W, H, "Figure 6. State diagram: StreetLight (LightMode)")

    def state(cx, cy, name, sub):
        w, h = 1.7, 0.75
        ax.add_patch(FancyBboxPatch((cx - w / 2, cy - h / 2), w, h, boxstyle="round,pad=0,rounding_size=0.18",
                                    fc=FILL, ec=INK, lw=1.3, zorder=2))
        txt(ax, cx, cy + 0.12, name, size=10, color=INK, fontweight="bold")
        txt(ax, cx, cy - 0.17, sub, size=7.6, color=NOTE)
        return dict(cx=cx, cy=cy, l=cx - w / 2, r=cx + w / 2, t=cy + h / 2, b=cy - h / 2)

    off = state(1.9, 4.2, "OFF", "0%  (daytime)")
    dim = state(5.2, 4.2, "DIM", "30%  (night)")
    full = state(8.5, 4.2, "FULL", "100%")
    fl = state(5.2, 1.7, "FLASHING", "60% warning")
    ft = state(8.5, 1.7, "FAULTY", "0%  broken")
    ax.add_patch(Circle((0.45, 4.2), 0.11, fc=INK, ec=INK, zorder=3))

    def arr(p, q, label, lx, ly, ha="center"):
        seg(ax, p, q, z=1); solid_arrow(ax, q, p)
        txt(ax, lx, ly, label, size=7.9, color="#111", ha=ha)

    arr((0.56, 4.2), (off["l"], 4.2), "", 0, 0)
    arr((off["r"], 4.35), (dim["l"], 4.35), "[night starts]", 3.55, 4.5)
    arr((dim["l"], 4.05), (off["r"], 4.05), "[day starts]", 3.55, 3.9)
    arr((dim["r"], 4.35), (full["l"], 4.35), "car passes / ambulance in area", 6.85, 4.5)
    arr((full["l"], 4.05), (dim["r"], 4.05), "after 3 ticks / ambulance gone", 6.85, 3.9)
    arr((5.05, dim["b"]), (5.05, fl["t"]), "accident\nreported", 4.95, 2.95, ha="right")
    arr((5.35, fl["t"]), (5.35, dim["b"]), "[road reopened]\n(OFF by day)", 5.45, 2.95, ha="left")
    arr((8.5, full["b"]), (8.5, ft["t"]), "random failure\n(0.2% per tick)", 8.6, 2.95, ha="left")
    arr((fl["r"], 1.7), (ft["l"], 1.7), "random failure", 6.85, 1.85)
    # repair loop back to OFF
    pts = [(8.5, ft["b"]), (8.5, 0.55), (1.9, 0.55), (1.9, off["b"])]
    for a, b in zip(pts, pts[1:]):
        seg(ax, a, b)
    solid_arrow(ax, pts[-1], pts[-2])
    txt(ax, 5.2, 0.7, "repair()  \u2014 done by the RepairCrew thread (producer-consumer queue)", size=7.9, color="#111")
    txt(ax, 0.35, 2.4, "A FAULTY light ignores\napplyMode() until it is\nrepaired (encapsulation:\nthe rule is enforced inside\nStreetLight itself).",
        size=7.8, ha="left", color=NOTE)
    fig.savefig(OUT / "fig6_state_streetlight.png"); plt.close(fig)

print("done", ONLY or "all")
