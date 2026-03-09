export interface User {
  id: number
  username: string
  realName: string
  role: string
  responsibility: string
  phone: string
  status: string
}

export interface Vehicle {
  id: number
  userId: number
  ownerName: string
  plateNumber: string
  brand: string
  color: string
  status: string
}

export interface ParkingLot {
  id: number
  name: string
  code: string
  address: string
  managerName: string
  businessHours: string
  totalSpaces: number
  occupiedSpaces: number
  freeSpaces: number
  status: string
}

export interface ParkingSpace {
  id: number
  parkingLotId: number
  code: string
  regionName: string
  category: string
  status: string
}

export interface PricingRule {
  id: number
  parkingLotId: number
  parkingLotName: string
  baseMinutes: number
  baseFee: number
  hourlyFee: number
  dailyCap: number
}

export interface ParkingRecord {
  id: number
  vehicleId: number
  plateNumber: string
  ownerName: string
  parkingLotId: number
  parkingLotName: string
  spaceCode: string
  entryTime: string
  exitTime: string | null
  durationMinutes: number
  amount: number
  status: string
}

export interface Order {
  id: number
  recordId: number
  plateNumber: string
  parkingLotName: string
  amount: number
  paymentStatus: string
  createdAt: string
}

export interface Notice {
  id: number
  title: string
  content: string
  level: string
}

export interface OperationLog {
  id: number
  action: string
  detail: string
  createdAt: string
}

export interface TeamMember {
  name: string
  role: string
  responsibility: string
}

export interface ProjectInfo {
  projectName: string
  team: TeamMember[]
  defaultAccount: User
}

export interface DashboardMetric {
  title: string
  value: string | number
  description: string
}

export interface TrendPoint {
  time: string
  entries: number
  exits: number
}

export interface TopParkingLot {
  name: string
  occupancyRate: string
  freeSpaces: number
}

export interface DashboardOverview {
  metrics: DashboardMetric[]
  occupancyRate: string
  parkingTrend: TrendPoint[]
  topParkingLots: TopParkingLot[]
}

export interface DispatchSuggestion {
  parkingLotName: string
  occupancyRate: string
  warningLevel: string
  action: string
}

export interface AppDataset {
  projectInfo: ProjectInfo
  overview: DashboardOverview
  dispatchSuggestions: DispatchSuggestion[]
  parkingLots: ParkingLot[]
  spaces: ParkingSpace[]
  pricingRules: PricingRule[]
  users: User[]
  vehicles: Vehicle[]
  records: ParkingRecord[]
  orders: Order[]
  notices: Notice[]
  logs: OperationLog[]
}

export interface VehiclePayload {
  userId: number
  plateNumber: string
  brand: string
  color: string
}

export interface ParkingLotPayload {
  name: string
  code: string
  address: string
  totalSpaces: number
  businessHours: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  user: User
}
