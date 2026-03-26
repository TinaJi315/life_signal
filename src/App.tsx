/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { 
  Shield, 
  Bell, 
  Home, 
  Users, 
  User, 
  CheckCircle2, 
  History, 
  Calendar, 
  MapPin, 
  Edit3, 
  Plus, 
  Trash2, 
  LogOut, 
  ChevronRight, 
  Phone, 
  MessageSquare, 
  AlertCircle,
  ArrowLeft,
  Flame,
  MoreVertical,
  Settings,
  UserPlus,
  Share2,
  Asterisk,
  Hourglass,
  Search,
  QrCode,
  Check,
  X,
  Info,
  Copy,
  ExternalLink,
  Sparkles,
  Loader2,
  Clock,
  Download
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { generateProfileImage } from './services/imageService';

// --- Types ---

type Page = 'home' | 'network' | 'profile' | 'success' | 'add-friend' | 'add-group' | 'friend-detail' | 'group-detail' | 'share' | 'add-contact' | 'remind-success' | 'notifications';

interface Contact {
  id: string;
  name: string;
  relation: string;
  phone: string;
  initials: string;
  colorClass: string;
}

interface Member {
  id: string;
  name: string;
  status: 'safe' | 'overdue';
  location: string;
  lastUpdated: string;
  image: string;
}

// --- Components ---

const BottomNav = ({ currentPage, setPage }: { currentPage: Page, setPage: (p: Page) => void }) => {
  const navItems = [
    { id: 'home', label: 'Home', icon: Home },
    { id: 'network', label: 'Network', icon: Users },
    { id: 'profile', label: 'Profile', icon: User },
  ];

  return (
    <nav className="fixed bottom-0 left-0 w-full z-50 flex justify-around items-center px-4 pb-8 pt-4 bg-surface-container-lowest shadow-[0_-4px_24px_rgba(25,28,33,0.06)] rounded-t-3xl">
      {navItems.map((item) => {
        const Icon = item.icon;
        const isActive = currentPage === item.id || (currentPage === 'success' && item.id === 'home');
        return (
          <button
            key={item.id}
            onClick={() => setPage(item.id as Page)}
            className={`flex flex-col items-center justify-center px-8 py-3 rounded-2xl transition-all duration-200 active:scale-90 ${
              isActive 
                ? 'bg-primary/10 text-primary' 
                : 'text-on-surface-variant hover:bg-surface-container'
            }`}
          >
            <Icon size={28} strokeWidth={isActive ? 2.5 : 2} />
            <span className="font-bold text-sm mt-1">{item.label}</span>
          </button>
        );
      })}
    </nav>
  );
};

const Header = ({ title, onNotificationClick }: { title: string, onNotificationClick: () => void }) => (
  <header className="fixed top-0 w-full z-50 flex justify-between items-center px-6 h-20 bg-surface-container-lowest">
    <div className="flex items-center gap-3">
      <Shield className="text-primary" size={32} strokeWidth={3} />
      <h1 className="font-black text-2xl uppercase tracking-tight text-primary">LifeSignal</h1>
    </div>
    <div className="flex items-center gap-2">
      <span className="font-bold text-primary mr-2">{title}</span>
      <button 
        onClick={onNotificationClick}
        className="p-2 rounded-full hover:bg-surface-container transition-colors active:scale-95"
      >
        <Bell className="text-on-surface-variant" size={24} />
      </button>
    </div>
  </header>
);

// --- Pages ---

const HomePage = ({ isCheckedIn, onCheckIn }: { isCheckedIn: boolean, onCheckIn: () => void }) => {
  if (isCheckedIn) {
    return (
      <div className="space-y-12">
        <section>
          <h2 className="text-6xl font-black leading-tight tracking-tighter text-secondary">Checked-in</h2>
          <p className="text-on-surface-variant text-xl mt-4 font-semibold">Your safety status has been updated.</p>
        </section>

        <section className="flex flex-col items-center justify-center py-16 bg-surface-container-low rounded-[4rem] border-4 border-surface-container shadow-sm">
          <div className="relative flex items-center justify-center">
            <div className="w-64 h-64 rounded-full bg-surface-container-highest flex flex-col items-center justify-center gap-4 border-8 border-surface-container opacity-80">
              <CheckCircle2 className="text-secondary" size={80} strokeWidth={3} />
              <span className="text-on-surface-variant font-bold text-2xl tracking-wide">Checked-in Today</span>
            </div>
            <div className="absolute inset-0 rounded-full bg-secondary opacity-5 blur-3xl -z-10"></div>
          </div>
        </section>

        <div className="grid grid-cols-1 gap-6">
          <div className="bg-surface-container-lowest p-8 rounded-3xl shadow-sm flex items-center justify-between">
            <div className="flex items-center gap-6">
              <div className="bg-primary/10 w-16 h-16 rounded-full flex items-center justify-center">
                <Calendar className="text-primary" size={32} />
              </div>
              <div>
                <p className="text-on-surface-variant font-semibold text-lg">Next Check-in</p>
                <p className="text-on-surface font-black text-2xl mt-1">Tomorrow 08:00 PM</p>
              </div>
            </div>
          </div>

          <div className="bg-surface-container p-8 rounded-3xl flex items-center justify-between">
            <div className="flex items-center gap-6">
              <div className="bg-secondary/10 w-16 h-16 rounded-full flex items-center justify-center">
                <History className="text-secondary" size={32} />
              </div>
              <div>
                <p className="text-on-surface-variant font-semibold text-lg">Last Record</p>
                <p className="text-on-surface font-black text-2xl mt-1">Today 08:15 AM</p>
              </div>
            </div>
            <div className="bg-secondary/20 text-secondary px-4 py-2 rounded-xl font-bold text-sm">
              Completed
            </div>
          </div>
        </div>

        <div className="p-8 border-l-8 border-primary bg-surface-container-low rounded-r-3xl">
          <p className="text-on-surface italic text-xl leading-relaxed">
            "Keeping in touch regularly is the best way to give your family peace of mind."
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-12">
      <section>
        <h2 className="text-5xl font-black leading-tight tracking-tighter">You haven't checked in today</h2>
        <div className="h-2 w-24 bg-tertiary rounded-full mt-4"></div>
      </section>

      <section className="flex justify-center items-center py-8">
        <div className="relative group">
          <div className="absolute inset-0 bg-secondary opacity-10 blur-3xl rounded-full transform group-active:scale-95 transition-all duration-300"></div>
          <button 
            onClick={onCheckIn}
            className="relative w-72 h-72 rounded-full bg-secondary text-white flex flex-col items-center justify-center shadow-[0_20px_50px_rgba(27,109,36,0.3)] active:scale-95 transition-all duration-200 border-[12px] border-secondary-container/30"
          >
            <span className="font-black text-4xl tracking-widest uppercase">Safe</span>
          </button>
        </div>
      </section>

      <section className="text-center">
        <p className="text-on-surface-variant font-medium text-xl uppercase tracking-widest mb-2">Time remaining to check in:</p>
        <div className="text-primary font-black text-5xl">
          4<span className="text-2xl font-bold ml-1">h</span> 20<span className="text-2xl font-bold ml-1">m</span>
        </div>
      </section>

      <article className="bg-surface-container-low rounded-3xl p-8 shadow-sm">
        <div className="flex flex-col gap-6">
          <div className="flex items-center justify-between">
            <div className="flex flex-col">
              <span className="text-on-surface-variant text-xs font-bold uppercase tracking-wider mb-1">Status</span>
              <span className="text-on-surface font-black text-xl">Last Check-in</span>
            </div>
            <div className="text-right">
              <span className="block text-secondary font-black text-xl">Today</span>
              <span className="block text-on-surface-variant text-lg font-bold">8:15 AM</span>
            </div>
          </div>
          <div className="h-px bg-outline-variant opacity-30"></div>
          <div className="flex items-center justify-between">
            <span className="text-on-surface font-bold text-lg">Previous</span>
            <div className="text-right">
              <span className="block text-on-surface font-bold text-lg">Yesterday</span>
              <span className="block text-on-surface-variant text-base font-medium">8:15 PM</span>
            </div>
          </div>
        </div>
      </article>
    </div>
  );
};

const NetworkPage = ({ setPage, setSelectedFriend, setSelectedGroup }: { setPage: (p: Page) => void, setSelectedFriend: (f: Member) => void, setSelectedGroup: (g: any) => void }) => {
  const friends: Member[] = [
    { id: '1', name: 'Sarah Miller', status: 'safe', location: 'Home', lastUpdated: '12m ago', image: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&h=400&fit=crop' },
    { id: '2', name: 'Arthur Chen', status: 'overdue', location: 'Community Center', lastUpdated: '4h ago', image: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=400&h=400&fit=crop' },
    { id: '3', name: 'Elena Rodriguez', status: 'safe', location: 'Office', lastUpdated: '1h ago', image: '' },
  ];

  const groups = [
    { id: 'g1', name: 'Family Circle', members: 4, avatars: friends.map(f => f.image) }
  ];

  return (
    <div className="space-y-12 pb-12">
      <section>
        <h2 className="text-4xl font-black text-on-surface tracking-tight">Your Network</h2>
      </section>

      {/* Friends Section */}
      <section className="space-y-6">
        <div className="flex items-center justify-between">
          <h3 className="text-2xl font-black text-on-surface tracking-tight flex items-center gap-2">
            <User size={24} className="text-primary" />
            Friends
          </h3>
          <button 
            onClick={() => setPage('add-friend')}
            className="flex items-center gap-2 bg-primary/10 text-primary px-4 py-2 rounded-xl font-bold text-sm active:scale-95 transition-all"
          >
            <UserPlus size={18} />
            Add Friend
          </button>
        </div>
        <div className="space-y-4">
          {friends.map((friend) => (
            <button 
              key={friend.id} 
              onClick={() => {
                setSelectedFriend(friend);
                setPage('friend-detail');
              }}
              className="w-full bg-surface-container-lowest p-5 rounded-3xl flex items-center justify-between border-2 border-surface-container/50 text-left active:scale-[0.98] transition-all"
            >
              <div className="flex items-center gap-4">
                <div className="relative">
                  <div className="w-16 h-16 rounded-2xl overflow-hidden bg-surface-container">
                    {friend.image ? (
                      <img src={friend.image} alt={friend.name} className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center bg-surface-container-highest">
                        <User className="text-on-surface-variant" size={24} />
                      </div>
                    )}
                  </div>
                  <div className={`absolute -bottom-1 -right-1 w-5 h-5 rounded-full border-4 border-surface-container-lowest ${
                    friend.status === 'safe' ? 'bg-secondary' : 'bg-tertiary'
                  }`}></div>
                </div>
                <div>
                  <h4 className="text-xl font-black text-on-surface">{friend.name}</h4>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className={`font-bold text-base ${friend.status === 'safe' ? 'text-secondary' : 'text-tertiary'}`}>
                      {friend.status === 'safe' ? 'Safe' : 'No Check-in'}
                    </span>
                    <span className="text-on-surface-variant font-medium text-sm">• {friend.lastUpdated}</span>
                  </div>
                </div>
              </div>
              <ChevronRight className="text-on-surface-variant/30" size={20} />
            </button>
          ))}
        </div>
      </section>

      {/* Groups Section */}
      <section className="space-y-6">
        <div className="flex items-center justify-between">
          <h3 className="text-2xl font-black text-on-surface tracking-tight flex items-center gap-2">
            <Users size={24} className="text-secondary" />
            Groups
          </h3>
          <button 
            onClick={() => setPage('add-group')}
            className="flex items-center gap-2 bg-secondary/10 text-secondary px-4 py-2 rounded-xl font-bold text-sm active:scale-95 transition-all"
          >
            <Plus size={18} />
            Add Group
          </button>
        </div>
        <div className="space-y-4">
          {groups.map((group) => (
            <div 
              key={group.id} 
              onClick={() => {
                setSelectedGroup(group);
                setPage('group-detail');
              }}
              className="bg-surface-container-lowest border-2 border-surface-container p-6 rounded-[2.5rem] shadow-sm space-y-6 cursor-pointer active:scale-[0.99] transition-all"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="text-2xl font-black text-on-surface">{group.name}</h4>
                  <p className="text-on-surface-variant font-bold">{group.members} Members</p>
                </div>
                <div className="flex -space-x-3">
                  {group.avatars.map((img, i) => (
                    <div key={i} className="w-10 h-10 rounded-full border-2 border-surface-container-lowest overflow-hidden bg-surface-container">
                      {img ? (
                        <img src={img} alt="" className="w-full h-full object-cover" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-[10px] font-bold bg-primary/10 text-primary">ER</div>
                      )}
                    </div>
                  ))}
                  <div className="w-10 h-10 rounded-full border-2 border-surface-container-lowest bg-primary flex items-center justify-center text-white text-[10px] font-black">
                    +1
                  </div>
                </div>
              </div>
              <button 
                onClick={(e) => {
                  e.stopPropagation();
                  // Trigger reminder logic
                }}
                className="w-full bg-[#1A237E] text-white py-5 rounded-2xl flex items-center justify-center gap-3 shadow-lg shadow-indigo-900/20 active:scale-95 transition-all"
              >
                <Bell size={24} fill="currentColor" />
                <span className="text-lg font-black uppercase tracking-widest">Remind All To Check-in</span>
              </button>
            </div>
          ))}
        </div>
      </section>

      {/* Safety Tip */}
      <section className="bg-[#A5F3A5] p-8 rounded-[2.5rem] relative overflow-hidden">
        <div className="relative z-10">
          <h4 className="text-2xl font-black text-[#1B5E20] mb-3">Safety Tip</h4>
          <p className="text-[#1B5E20] text-lg font-bold leading-relaxed">
            Groups allow you to monitor multiple people at once and send bulk reminders with a single tap.
          </p>
        </div>
        <Shield className="absolute top-1/2 right-4 -translate-y-1/2 text-[#1B5E20]/10 w-32 h-32" strokeWidth={1} />
      </section>
    </div>
  );
};

const AddFriendPage = ({ setPage, onBack }: { setPage: (p: Page) => void, onBack: () => void }) => {
  const [addedIds, setAddedIds] = useState<number[]>([]);

  const handleAdd = (id: number) => {
    setAddedIds(prev => [...prev, id]);
  };

  return (
    <div className="space-y-12">
      <header className="flex items-center gap-4">
        <button onClick={onBack} className="p-2 rounded-full hover:bg-surface-container active:scale-90 transition-all">
          <ArrowLeft size={32} strokeWidth={3} />
        </button>
        <h2 className="text-4xl font-black tracking-tight">Add Friend</h2>
      </header>

      <section className="space-y-6">
        <div className="relative">
          <Search className="absolute left-6 top-1/2 -translate-y-1/2 text-on-surface-variant" size={24} />
          <input 
            type="text" 
            placeholder="Search by name or phone..." 
            className="w-full bg-surface-container-lowest border-4 border-surface-container rounded-3xl p-6 pl-16 text-xl font-bold focus:border-primary outline-none transition-all"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <button className="bg-primary text-white p-8 rounded-3xl flex flex-col items-center gap-4 active:scale-95 transition-all shadow-lg shadow-primary/20">
            <QrCode size={48} />
            <span className="font-black text-lg">Scan QR</span>
          </button>
          <button 
            onClick={() => setPage('share')}
            className="bg-surface-container-lowest border-4 border-surface-container p-8 rounded-3xl flex flex-col items-center gap-4 active:scale-95 transition-all"
          >
            <Share2 size={48} className="text-primary" />
            <span className="font-black text-lg">Share Link</span>
          </button>
        </div>
      </section>

      <section className="space-y-4">
        <h3 className="text-xl font-black text-on-surface-variant uppercase tracking-widest">Suggested</h3>
        <div className="space-y-3">
          {[
            { id: 1, name: 'Robert Vance', relation: 'Cousin', image: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&h=400&fit=crop' },
            { id: 2, name: 'Linda Smith', relation: 'Neighbor', image: '' }
          ].map((person) => (
            <div key={person.id} className="bg-surface-container-lowest p-5 rounded-3xl flex items-center justify-between border-2 border-surface-container/30">
              <div className="flex items-center gap-4">
                <div className="w-14 h-14 rounded-2xl overflow-hidden bg-surface-container">
                  {person.image ? <img src={person.image} alt="" className="w-full h-full object-cover" /> : <User className="m-auto mt-3 text-on-surface-variant" />}
                </div>
                <div>
                  <h4 className="text-lg font-black">{person.name}</h4>
                  <p className="text-on-surface-variant font-bold text-sm">{person.relation}</p>
                </div>
              </div>
              <button 
                onClick={() => handleAdd(person.id)}
                disabled={addedIds.includes(person.id)}
                className={`px-6 py-2 rounded-xl font-black text-sm transition-all active:scale-90 ${
                  addedIds.includes(person.id) 
                    ? 'bg-secondary/20 text-secondary' 
                    : 'bg-primary text-white'
                }`}
              >
                {addedIds.includes(person.id) ? 'Added' : 'Add'}
              </button>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};

const AddGroupPage = ({ onBack }: { onBack: () => void }) => {
  const friends = [
    { id: '1', name: 'Sarah Miller', image: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&h=400&fit=crop' },
    { id: '2', name: 'Arthur Chen', image: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=400&h=400&fit=crop' },
    { id: '3', name: 'Elena Rodriguez', image: '' },
  ];

  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  const toggleMember = (id: string) => {
    setSelectedIds(prev => 
      prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]
    );
  };

  return (
    <div className="space-y-12">
      <header className="flex items-center gap-4">
        <button onClick={onBack} className="p-2 rounded-full hover:bg-surface-container active:scale-90 transition-all">
          <ArrowLeft size={32} strokeWidth={3} />
        </button>
        <h2 className="text-4xl font-black tracking-tight">New Group</h2>
      </header>

      <section className="space-y-8">
        <div>
          <label className="block text-sm font-bold text-on-surface-variant uppercase tracking-widest mb-3">Group Name</label>
          <input 
            type="text" 
            placeholder="e.g. Sunday Hiking Club" 
            className="w-full bg-surface-container-lowest border-4 border-surface-container rounded-3xl p-6 text-2xl font-black focus:border-secondary outline-none transition-all"
          />
        </div>

        <div className="space-y-4">
          <label className="block text-sm font-bold text-on-surface-variant uppercase tracking-widest mb-3">Select Members</label>
          <div className="space-y-3">
            {friends.map((friend) => (
              <button 
                key={friend.id} 
                onClick={() => toggleMember(friend.id)}
                className={`w-full bg-surface-container-lowest p-5 rounded-3xl flex items-center justify-between border-2 transition-all ${
                  selectedIds.includes(friend.id) ? 'border-secondary bg-secondary/5' : 'border-surface-container/30'
                }`}
              >
                <div className="flex items-center gap-4">
                  <div className="w-14 h-14 rounded-2xl overflow-hidden bg-surface-container">
                    {friend.image ? <img src={friend.image} alt="" className="w-full h-full object-cover" /> : <User className="m-auto mt-3 text-on-surface-variant" />}
                  </div>
                  <h4 className="text-lg font-black">{friend.name}</h4>
                </div>
                <div className={`w-8 h-8 rounded-full border-4 flex items-center justify-center transition-all ${
                  selectedIds.includes(friend.id) ? 'border-secondary bg-secondary' : 'border-surface-container'
                }`}>
                  {selectedIds.includes(friend.id) && <Check size={16} className="text-white" strokeWidth={4} />}
                </div>
              </button>
            ))}
          </div>
        </div>
      </section>

      <button 
        onClick={onBack}
        className="w-full bg-secondary text-white py-6 rounded-2xl font-black text-2xl shadow-xl shadow-secondary/20 active:scale-95 transition-all"
      >
        Create Group
      </button>
    </div>
  );
};

const FriendDetailPage = ({ friend, onBack }: { friend: Member, onBack: () => void }) => {
  const [showProfile, setShowProfile] = useState(false);

  if (showProfile) {
    return (
      <div className="space-y-12">
        <header className="flex items-center gap-4">
          <button onClick={() => setShowProfile(false)} className="p-2 rounded-full hover:bg-surface-container active:scale-90 transition-all">
            <ArrowLeft size={32} strokeWidth={3} />
          </button>
          <h2 className="text-4xl font-black tracking-tight">Profile</h2>
        </header>

        <section className="flex flex-col items-center text-center">
          <div className="w-48 h-48 rounded-[3rem] overflow-hidden bg-surface-container border-8 border-surface-container-lowest shadow-2xl mb-8">
            {friend.image ? (
              <img src={friend.image} alt="" className="w-full h-full object-cover" referrerPolicy="no-referrer" />
            ) : (
              <User className="m-auto mt-12 text-on-surface-variant" size={80} />
            )}
          </div>
          <h3 className="text-5xl font-black tracking-tighter mb-2">{friend.name}</h3>
          <p className="text-on-surface-variant text-xl font-bold uppercase tracking-widest">Verified Member</p>
        </section>

        <section className="bg-surface-container-lowest p-8 rounded-[3rem] border-2 border-surface-container space-y-6">
          <div className="flex items-center justify-between p-4 border-b border-surface-container">
            <span className="text-on-surface-variant font-bold">Phone</span>
            <span className="font-black text-xl">+1 (555) 000-0000</span>
          </div>
          <div className="flex items-center justify-between p-4 border-b border-surface-container">
            <span className="text-on-surface-variant font-bold">Email</span>
            <span className="font-black text-xl">{friend.name.toLowerCase().replace(' ', '.')}@example.com</span>
          </div>
          <div className="flex items-center justify-between p-4">
            <span className="text-on-surface-variant font-bold">Member Since</span>
            <span className="font-black text-xl">Jan 2024</span>
          </div>
        </section>

        <button 
          onClick={() => setShowProfile(false)}
          className="w-full bg-primary text-white py-6 rounded-2xl font-black text-2xl shadow-xl shadow-primary/20 active:scale-95 transition-all"
        >
          Done
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-12">
      <header className="flex items-center justify-between">
        <button onClick={onBack} className="p-2 rounded-full hover:bg-surface-container active:scale-90 transition-all">
          <ArrowLeft size={32} strokeWidth={3} />
        </button>
        <button className="p-2 rounded-full hover:bg-surface-container active:scale-90 transition-all text-on-surface-variant">
          <MoreVertical size={28} />
        </button>
      </header>

      <section className="flex flex-col items-center text-center">
        <div className="relative mb-6">
          <div className="w-40 h-40 rounded-[2.5rem] overflow-hidden bg-surface-container border-8 border-surface-container-lowest shadow-2xl">
            {friend.image ? <img src={friend.image} alt="" className="w-full h-full object-cover" /> : <User size={64} className="m-auto mt-10 text-on-surface-variant" />}
          </div>
          <div className={`absolute -bottom-2 -right-2 w-10 h-10 rounded-full border-4 border-surface-container-lowest shadow-lg ${
            friend.status === 'safe' ? 'bg-secondary' : 'bg-tertiary'
          }`}></div>
        </div>
        <h2 className="text-5xl font-black tracking-tight mb-2">{friend.name}</h2>
        <p className="text-on-surface-variant text-xl font-bold uppercase tracking-widest">
          {friend.status === 'safe' ? 'Currently Safe' : 'No Recent Check-in'}
        </p>
      </section>

      <div className="grid grid-cols-1 gap-4">
        <div className="bg-surface-container-lowest p-8 rounded-[2.5rem] border-2 border-surface-container flex items-center gap-6">
          <div className="w-16 h-16 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
            <Clock size={32} />
          </div>
          <div>
            <p className="text-sm font-bold text-on-surface-variant uppercase tracking-widest">Last Check-in</p>
            <p className="text-2xl font-black">{friend.lastUpdated}</p>
            <p className="text-on-surface-variant font-bold">Today</p>
          </div>
        </div>

        <div className="bg-surface-container-lowest p-8 rounded-[2.5rem] border-2 border-surface-container flex items-center gap-6">
          <div className="w-16 h-16 bg-secondary/10 rounded-2xl flex items-center justify-center text-secondary">
            <History size={32} />
          </div>
          <div>
            <p className="text-sm font-bold text-on-surface-variant uppercase tracking-widest">Check-in History</p>
            <p className="text-2xl font-black">100% Success Rate</p>
            <p className="text-on-surface-variant font-bold">Last 30 days</p>
          </div>
        </div>
      </div>

      <div className="flex gap-4">
        <button 
          onClick={() => setShowProfile(true)}
          className="flex-1 bg-surface-container-highest text-on-surface py-5 rounded-2xl font-black text-xl active:scale-95 transition-all"
        >
          View Profile
        </button>
        <button 
          onClick={() => {
            // In a real app, this would trigger a confirmation modal
            // For now, we'll just go back to simulate removal
            onBack();
          }}
          className="flex-1 bg-tertiary/10 text-tertiary py-5 rounded-2xl font-black text-xl active:scale-95 transition-all"
        >
          Remove
        </button>
      </div>
    </div>
  );
};

const GroupDetailPage = ({ group, onBack, onRemindAll }: { group: any, onBack: () => void, onRemindAll: () => void }) => {
  return (
    <div className="space-y-12">
      <header className="flex items-center justify-between">
        <button onClick={onBack} className="p-2 rounded-full hover:bg-surface-container active:scale-90 transition-all">
          <ArrowLeft size={32} strokeWidth={3} />
        </button>
      </header>

      <section>
        <h2 className="text-6xl font-black tracking-tighter leading-none mb-4">{group.name}</h2>
        <div className="flex items-center gap-4">
          <div className="flex -space-x-3">
            {group.avatars.map((img: string, i: number) => (
              <div key={i} className="w-12 h-12 rounded-full border-4 border-surface-container-lowest overflow-hidden bg-surface-container">
                {img ? <img src={img} alt="" className="w-full h-full object-cover" /> : <User className="m-auto mt-2 text-on-surface-variant" size={20} />}
              </div>
            ))}
          </div>
          <span className="text-xl font-bold text-on-surface-variant">{group.members} Members</span>
        </div>
      </section>

      <button 
        onClick={onRemindAll}
        className="w-full bg-[#1A237E] text-white py-8 rounded-[2.5rem] flex flex-col items-center gap-4 shadow-2xl shadow-indigo-900/40 active:scale-95 transition-all"
      >
        <Bell size={48} fill="currentColor" />
        <span className="text-2xl font-black uppercase tracking-[0.2em]">Remind All Members</span>
      </button>

      <section className="space-y-6">
        <h3 className="text-2xl font-black tracking-tight">Member Status</h3>
        <div className="space-y-4">
          {[
            { name: 'Sarah Miller', status: 'safe' },
            { name: 'Arthur Chen', status: 'overdue' },
            { name: 'Elena Rodriguez', status: 'safe' },
            { name: 'Marcus Vance', status: 'safe' }
          ].map((m, i) => (
            <div key={i} className="bg-surface-container-lowest p-6 rounded-3xl flex items-center justify-between border-2 border-surface-container/30">
              <div className="flex items-center gap-4">
                <div className={`w-4 h-4 rounded-full ${m.status === 'safe' ? 'bg-secondary' : 'bg-tertiary'}`}></div>
                <span className="text-xl font-bold">{m.name}</span>
              </div>
              <span className={`font-black uppercase tracking-widest text-sm ${m.status === 'safe' ? 'text-secondary' : 'text-tertiary'}`}>
                {m.status === 'safe' ? 'Safe' : 'Overdue'}
              </span>
            </div>
          ))}
        </div>
      </section>

      <button className="w-full flex items-center justify-center gap-3 bg-surface-container-highest text-on-surface py-6 rounded-2xl font-black text-xl active:scale-95 transition-all">
        <UserPlus size={24} />
        Add Member
      </button>
    </div>
  );
};

const RemindSuccessPage = ({ onBack }: { onBack: () => void }) => {
  return (
    <motion.div 
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[100] bg-[#1A237E] flex flex-col items-center justify-center p-8 text-center"
    >
      <motion.div 
        initial={{ scale: 0, rotate: -180 }}
        animate={{ scale: 1, rotate: 0 }}
        transition={{ type: "spring", damping: 12, stiffness: 100 }}
        className="w-48 h-48 rounded-full bg-white/20 flex items-center justify-center mb-12 relative"
      >
        <div className="absolute inset-0 rounded-full border-4 border-white/30 animate-ping"></div>
        <Bell className="text-white" size={80} strokeWidth={3} />
      </motion.div>
      
      <div className="space-y-6">
        <h2 className="text-6xl font-black text-white tracking-tighter leading-tight">Reminders Sent!</h2>
        <p className="text-white/80 text-2xl font-bold max-w-md mx-auto leading-relaxed">
          We've sent a signal to all group members to check in.
        </p>
      </div>

      <button 
        onClick={onBack}
        className="mt-12 bg-white text-[#1A237E] px-12 py-5 rounded-2xl font-black text-2xl shadow-2xl active:scale-95 transition-all"
      >
        Back to Group
      </button>
    </motion.div>
  );
};

const ProfilePage = ({ setPage, profileImage, setProfileImage }: { setPage: (p: Page) => void, profileImage: string, setProfileImage: (img: string) => void }) => {
  const fileInputRef = React.useRef<HTMLInputElement>(null);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setProfileImage(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const contacts: Contact[] = [
    { id: '1', name: 'Sarah Miller', relation: 'Daughter', phone: '+1 (555) 123-4567', initials: 'SM', colorClass: 'bg-primary' },
    { id: '2', name: 'Arthur Chen', relation: 'Son', phone: '+1 (555) 987-6543', initials: 'AC', colorClass: 'bg-secondary' },
  ];

  return (
    <div className="space-y-12 pb-12">
      <section className="flex flex-col items-center text-center">
        <div className="relative mb-6">
          <div className="w-40 h-40 rounded-full overflow-hidden bg-surface-container border-8 border-surface-container-lowest shadow-2xl relative">
            <img 
              src={profileImage} 
              alt="Profile" 
              className="w-full h-full object-cover" 
              referrerPolicy="no-referrer"
            />
          </div>
          <input 
            type="file" 
            ref={fileInputRef} 
            onChange={handleImageChange} 
            accept="image/*" 
            className="hidden" 
          />
          <button 
            onClick={() => fileInputRef.current?.click()}
            className="absolute bottom-2 right-2 bg-primary text-white p-3 rounded-2xl shadow-lg active:scale-90 transition-all"
            title="Upload Photo"
          >
            <Edit3 size={24} />
          </button>
        </div>
        <h2 className="text-5xl font-black tracking-tight mb-2">Leon S. Kennedy</h2>
        
        <div className="flex gap-4 mt-6">
          <button 
            onClick={() => setPage('share')}
            className="flex items-center gap-2 bg-primary/10 text-primary px-8 py-4 rounded-2xl font-black active:scale-95 transition-all"
          >
            <QrCode size={24} />
            Share Profile
          </button>
        </div>
      </section>

      <div className="grid grid-cols-1 gap-6">
        <section className="bg-surface-container-lowest p-8 rounded-[2.5rem] border-2 border-surface-container space-y-6 shadow-sm">
          <div className="flex items-center justify-between">
            <h3 className="text-2xl font-black flex items-center gap-3">
              <Users className="text-secondary" size={28} />
              Emergency Contacts
            </h3>
            <button 
              onClick={() => setPage('add-contact')}
              className="bg-secondary/10 text-secondary p-3 rounded-xl active:scale-90 transition-all"
            >
              <Plus size={28} />
            </button>
          </div>
          <div className="space-y-4">
            {contacts.map((contact) => (
              <div key={contact.id} className="flex items-center justify-between p-4 bg-surface-container-low rounded-2xl">
                <div className="flex items-center gap-4">
                  <div className={`${contact.colorClass} w-14 h-14 rounded-2xl flex items-center justify-center text-white font-black text-xl`}>
                    {contact.initials}
                  </div>
                  <div>
                    <h4 className="font-black text-lg">{contact.name}</h4>
                    <p className="text-on-surface-variant font-bold text-sm">{contact.relation}</p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <button className="p-3 bg-surface-container rounded-xl text-on-surface-variant active:scale-90 transition-all">
                    <Phone size={20} />
                  </button>
                  <button className="p-3 bg-surface-container rounded-xl text-on-surface-variant active:scale-90 transition-all">
                    <MessageSquare size={20} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="bg-surface-container-lowest p-8 rounded-[2.5rem] border-2 border-surface-container space-y-6 shadow-sm">
          <div className="flex items-center justify-between">
            <h3 className="text-2xl font-black flex items-center gap-3">
              <Settings className="text-on-surface-variant" size={28} />
              Account Settings
            </h3>
          </div>
          <div className="space-y-2">
            <button className="w-full flex items-center justify-between p-4 hover:bg-surface-container rounded-2xl transition-colors group">
              <span className="font-bold text-lg">Privacy & Security</span>
              <ChevronRight className="text-on-surface-variant group-hover:translate-x-1 transition-transform" />
            </button>
            <button className="w-full flex items-center justify-between p-4 hover:bg-surface-container rounded-2xl transition-colors group">
              <span className="font-bold text-lg">Notification Preferences</span>
              <ChevronRight className="text-on-surface-variant group-hover:translate-x-1 transition-transform" />
            </button>
            <button className="w-full flex items-center justify-between p-4 hover:bg-surface-container rounded-2xl transition-colors group text-tertiary">
              <span className="font-bold text-lg">Sign Out</span>
              <LogOut size={20} />
            </button>
            
            <div className="h-px bg-surface-container my-2"></div>
            
            <button 
              onClick={() => window.open('/api/export', '_blank')}
              className="w-full flex items-center justify-between p-4 bg-primary/10 hover:bg-primary/20 rounded-2xl transition-colors group text-primary"
            >
              <div className="flex flex-col items-start">
                <span className="font-black text-lg">Download Project (ZIP)</span>
                <p className="text-xs font-bold opacity-70">Export all source code files</p>
              </div>
              <Download size={24} strokeWidth={3} />
            </button>
          </div>
        </section>
      </div>
    </div>
  );
};

const AddContactPage = ({ onBack }: { onBack: () => void }) => {
  return (
    <div className="space-y-8">
      <div className="flex items-center gap-4">
        <button onClick={onBack} className="p-2 bg-surface-container rounded-xl active:scale-90 transition-all">
          <ArrowLeft size={24} />
        </button>
        <h2 className="text-3xl font-black">Add Contact</h2>
      </div>

      <div className="space-y-6">
        <div className="space-y-2">
          <label className="text-sm font-black uppercase tracking-widest text-on-surface-variant px-2">Name</label>
          <input 
            type="text" 
            placeholder="Contact Name"
            className="w-full bg-surface-container p-6 rounded-2xl font-bold text-lg outline-none focus:ring-4 ring-primary/20 transition-all"
          />
        </div>
        <div className="space-y-2">
          <label className="text-sm font-black uppercase tracking-widest text-on-surface-variant px-2">Relation</label>
          <input 
            type="text" 
            placeholder="e.g. Family, Friend"
            className="w-full bg-surface-container p-6 rounded-2xl font-bold text-lg outline-none focus:ring-4 ring-primary/20 transition-all"
          />
        </div>
        <div className="space-y-2">
          <label className="text-sm font-black uppercase tracking-widest text-on-surface-variant px-2">Phone Number</label>
          <input 
            type="tel" 
            placeholder="+1 (555) 000-0000"
            className="w-full bg-surface-container p-6 rounded-2xl font-bold text-lg outline-none focus:ring-4 ring-primary/20 transition-all"
          />
        </div>

        <button className="w-full bg-primary text-white py-6 rounded-3xl font-black text-xl shadow-xl shadow-primary/20 active:scale-95 transition-all mt-8">
          Save Contact
        </button>
      </div>
    </div>
  );
};

const SuccessPage = ({ onBack }: { onBack: () => void }) => {
  const [countdown, setCountdown] = useState(5);

  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    } else {
      onBack();
    }
  }, [countdown, onBack]);

  return (
    <motion.div 
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      className="fixed inset-0 z-[100] bg-secondary flex flex-col items-center justify-center p-8 text-center"
    >
      <div className="w-48 h-48 bg-white/20 rounded-full flex items-center justify-center mb-12 animate-pulse">
        <CheckCircle2 className="text-white" size={100} strokeWidth={3} />
      </div>
      
      <h2 className="text-6xl font-black text-white tracking-tighter mb-6">Status Updated!</h2>
      <p className="text-white/80 text-2xl font-bold max-w-md leading-relaxed mb-12">
        Your network has been notified that you are safe.
      </p>

      <div className="bg-white/10 px-8 py-4 rounded-3xl backdrop-blur-md">
        <p className="text-white font-black text-xl uppercase tracking-widest">
          Returning home in {countdown}s
        </p>
      </div>

      <button 
        onClick={onBack}
        className="mt-12 bg-white text-secondary px-12 py-5 rounded-2xl font-black text-2xl shadow-2xl active:scale-95 transition-all"
      >
        Done
      </button>
    </motion.div>
  );
};

const SharePage = ({ onBack }: { onBack: () => void }) => {
  const shareUrl = "https://lifesignal.app/invite/james-wilson-123";
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleShare = async () => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: 'Join my LifeSignal Network',
          text: 'Add me on LifeSignal to keep each other safe!',
          url: shareUrl,
        });
      } catch (err) {
        console.error('Error sharing:', err);
      }
    }
  };

  return (
    <div className="space-y-12">
      <header className="flex items-center gap-4">
        <button onClick={onBack} className="p-2 rounded-full hover:bg-surface-container active:scale-90 transition-all">
          <ArrowLeft size={32} strokeWidth={3} />
        </button>
        <h2 className="text-4xl font-black tracking-tight">Share Profile</h2>
      </header>

      <section className="flex flex-col items-center text-center space-y-8">
        <div className="p-8 bg-white rounded-[3rem] shadow-2xl shadow-primary/10 border-8 border-surface-container-lowest">
          <QRCodeSVG 
            value={shareUrl} 
            size={240}
            level="H"
            includeMargin={false}
          />
        </div>
        
        <div className="space-y-2">
          <h3 className="text-2xl font-black">Your Personal QR Code</h3>
          <p className="text-on-surface-variant font-bold max-w-xs mx-auto">
            Let friends scan this to add you instantly to their network.
          </p>
        </div>
      </section>

      <section className="space-y-6">
        <div className="space-y-3">
          <label className="block text-sm font-bold text-on-surface-variant uppercase tracking-widest">Shareable Link</label>
          <div className="flex gap-3">
            <div className="flex-1 bg-surface-container-lowest border-4 border-surface-container rounded-2xl p-4 font-bold text-on-surface-variant truncate">
              {shareUrl}
            </div>
            <button 
              onClick={handleCopy}
              className={`p-4 rounded-2xl transition-all active:scale-90 ${
                copied ? 'bg-secondary text-white' : 'bg-primary text-white'
              }`}
            >
              {copied ? <Check size={24} /> : <Copy size={24} />}
            </button>
          </div>
        </div>

        <button 
          onClick={handleShare}
          className="w-full bg-surface-container-highest text-on-surface py-6 rounded-2xl font-black text-xl flex items-center justify-center gap-3 active:scale-95 transition-all"
        >
          <ExternalLink size={24} />
          More Share Options
        </button>
      </section>

      <div className="bg-primary/5 p-8 rounded-[2.5rem] border-2 border-primary/10">
        <p className="text-primary font-bold text-center leading-relaxed">
          Sharing your profile allows friends to monitor your safety status and receive alerts if you miss a check-in.
        </p>
      </div>
    </div>
  );
};

const NotificationSettingsPage = ({ onBack }: { onBack: () => void }) => {
  const [settings, setSettings] = useState({
    missedCheckIn: true,
    groupReminder: true,
    newFriend: true,
    systemAlerts: true,
  });

  const toggle = (key: keyof typeof settings) => {
    setSettings(prev => ({ ...prev, [key]: !prev[key] }));
  };

  return (
    <div className="space-y-12">
      <header className="flex items-center gap-4">
        <button onClick={onBack} className="p-2 rounded-full hover:bg-surface-container active:scale-90 transition-all">
          <ArrowLeft size={32} strokeWidth={3} />
        </button>
        <h2 className="text-4xl font-black tracking-tight">Notifications</h2>
      </header>

      <section className="space-y-6">
        <div className="bg-surface-container-lowest p-8 rounded-[2.5rem] border-2 border-surface-container space-y-8 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="space-y-1">
              <h4 className="text-xl font-black">Missed Check-ins</h4>
              <p className="text-on-surface-variant font-bold">Alert when someone in your network is overdue.</p>
            </div>
            <button 
              onClick={() => toggle('missedCheckIn')}
              className={`w-16 h-8 rounded-full transition-colors relative ${settings.missedCheckIn ? 'bg-primary' : 'bg-surface-container-highest'}`}
            >
              <div className={`absolute top-1 w-6 h-6 rounded-full bg-white transition-all ${settings.missedCheckIn ? 'left-9' : 'left-1'}`}></div>
            </button>
          </div>

          <div className="h-px bg-surface-container"></div>

          <div className="flex items-center justify-between">
            <div className="space-y-1">
              <h4 className="text-xl font-black">Group Reminders</h4>
              <p className="text-on-surface-variant font-bold">Get notified when a group leader sends a reminder.</p>
            </div>
            <button 
              onClick={() => toggle('groupReminder')}
              className={`w-16 h-8 rounded-full transition-colors relative ${settings.groupReminder ? 'bg-primary' : 'bg-surface-container-highest'}`}
            >
              <div className={`absolute top-1 w-6 h-6 rounded-full bg-white transition-all ${settings.groupReminder ? 'left-9' : 'left-1'}`}></div>
            </button>
          </div>

          <div className="h-px bg-surface-container"></div>

          <div className="flex items-center justify-between">
            <div className="space-y-1">
              <h4 className="text-xl font-black">New Friend Requests</h4>
              <p className="text-on-surface-variant font-bold">When someone wants to join your network.</p>
            </div>
            <button 
              onClick={() => toggle('newFriend')}
              className={`w-16 h-8 rounded-full transition-colors relative ${settings.newFriend ? 'bg-primary' : 'bg-surface-container-highest'}`}
            >
              <div className={`absolute top-1 w-6 h-6 rounded-full bg-white transition-all ${settings.newFriend ? 'left-9' : 'left-1'}`}></div>
            </button>
          </div>

          <div className="h-px bg-surface-container"></div>

          <div className="flex items-center justify-between">
            <div className="space-y-1">
              <h4 className="text-xl font-black">System Alerts</h4>
              <p className="text-on-surface-variant font-bold">Important security and app updates.</p>
            </div>
            <button 
              onClick={() => toggle('systemAlerts')}
              className={`w-16 h-8 rounded-full transition-colors relative ${settings.systemAlerts ? 'bg-primary' : 'bg-surface-container-highest'}`}
            >
              <div className={`absolute top-1 w-6 h-6 rounded-full bg-white transition-all ${settings.systemAlerts ? 'left-9' : 'left-1'}`}></div>
            </button>
          </div>
        </div>
      </section>

      <div className="bg-primary/5 p-8 rounded-[2.5rem] border-2 border-primary/10">
        <div className="flex gap-4">
          <Info className="text-primary shrink-0" size={24} />
          <p className="text-primary font-bold leading-relaxed">
            We recommend keeping "Missed Check-ins" enabled to ensure you can respond quickly to potential emergencies.
          </p>
        </div>
      </div>
    </div>
  );
};

export default function App() {
  const [page, setPage] = useState<Page>('home');
  const [isCheckedIn, setIsCheckedIn] = useState(false);
  const [selectedFriend, setSelectedFriend] = useState<Member | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<any | null>(null);
  const [profileImage, setProfileImage] = useState('https://images.unsplash.com/photo-1595590424283-b8f17842773f?w=1000&h=1000&fit=crop'); // Tactical placeholder

  const handleCheckIn = () => {
    setPage('success');
    setIsCheckedIn(true);
  };

  const getTitle = () => {
    switch (page) {
      case 'home': return 'Home';
      case 'network': return 'Network';
      case 'profile': return 'Profile';
      case 'notifications': return 'Notifications';
      default: return '';
    }
  };

  const renderPage = () => {
    switch (page) {
      case 'home': return <HomePage isCheckedIn={isCheckedIn} onCheckIn={handleCheckIn} />;
      case 'network': return <NetworkPage setPage={setPage} setSelectedFriend={setSelectedFriend} setSelectedGroup={setSelectedGroup} />;
      case 'profile': return <ProfilePage setPage={setPage} profileImage={profileImage} setProfileImage={setProfileImage} />;
      case 'add-contact': return <AddContactPage onBack={() => setPage('profile')} />;
      case 'add-friend': return <AddFriendPage setPage={setPage} onBack={() => setPage('network')} />;
      case 'add-group': return <AddGroupPage onBack={() => setPage('network')} />;
      case 'friend-detail': return selectedFriend ? <FriendDetailPage friend={selectedFriend} onBack={() => setPage('network')} /> : null;
      case 'group-detail': return selectedGroup ? <GroupDetailPage group={selectedGroup} onBack={() => setPage('network')} onRemindAll={() => setPage('remind-success')} /> : null;
      case 'share': return <SharePage onBack={() => setPage('network')} />;
      case 'notifications': return <NotificationSettingsPage onBack={() => setPage('network')} />;
      default: return null;
    }
  };

  const showBottomNav = ['home', 'network', 'profile'].includes(page);

  return (
    <div className="min-h-screen">
      <AnimatePresence mode="wait">
        {page === 'success' ? (
          <SuccessPage onBack={() => setPage('home')} />
        ) : page === 'remind-success' ? (
          <RemindSuccessPage onBack={() => setPage('group-detail')} />
        ) : (
          <motion.div
            key={page}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className={`pb-32 ${showBottomNav ? 'pt-24' : 'pt-12'}`}
          >
            {showBottomNav && <Header title={getTitle()} onNotificationClick={() => setPage('notifications')} />}
            <main className="max-w-4xl mx-auto px-6">
              {renderPage()}
            </main>
            {showBottomNav && <BottomNav currentPage={page} setPage={setPage} />}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Background Decorative Element */}
      <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none opacity-10">
        <img 
          alt="" 
          className="w-full h-full object-cover" 
          src="https://images.unsplash.com/photo-1557683316-973673baf926?w=1920&q=80" 
        />
      </div>
    </div>
  );
}
