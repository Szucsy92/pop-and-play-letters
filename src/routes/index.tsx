import { createFileRoute } from '@tanstack/react-router';
import { KeypadGame } from '@/components/KeypadGame';
import { ChildLockWrapper } from '@/components/ChildLockWrapper';

export const Route = createFileRoute('/')({
  component: Index,
});

function Index() {
  return (
    <main className="w-screen h-screen overflow-hidden bg-background">
      <ChildLockWrapper>
        <KeypadGame />
      </ChildLockWrapper>
    </main>
  );
}
